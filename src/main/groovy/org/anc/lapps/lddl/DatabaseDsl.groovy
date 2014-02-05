package org.anc.lapps.lddl

import groovy.sql.Sql
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer

/**
 * @author Keith Suderman
 */
class DatabaseDsl {
    File parentDir
    DatabaseDelegate databaseInfo = new DatabaseDelegate()
    List statements = []
    Set<String> included = new HashSet<String>()
    Sql sql
    Binding bindings = new Binding()
    static boolean debug = false

    void run(File file, args) {
        parentDir = file.parentFile
        included << file.name
        run(file.text, args)
    }

    void run(String scriptString, args) {
        ClassLoader loader = Thread.currentThread().contextClassLoader;
        if (loader == null) {
            loader = this.class.classLoader
        }

        // Add some default imports for the user.
        ImportCustomizer customizer = new ImportCustomizer()
        def packages = [
                'groovy.sql',
                'org.postgresql'
        ]
        packages.each {
            customizer.addStarImports(it)
        }

        CompilerConfiguration configuration = new CompilerConfiguration()
        configuration.addCompilationCustomizers(customizer)
        GroovyShell shell = new GroovyShell(loader, bindings, configuration)

        Script script = shell.parse(scriptString)
        if (args != null && args.size() > 0) {
            script.binding.setVariable("args", args)
        }
        else {
            script.binding.setVariable('args', [])
        }

        // Running the script will generate a list of SQL statements
        // needed to initialize the database.
        script.metaClass = getMetaClass(script.class,shell)
        script.run()

        // Make sure the DB is configured. If we don't have a database
        // connection there is no sense running SQL statements.
        try {
            databaseInfo.validateFields()
        }
        catch (Exception e) {
            println "No database configuration has been provided."
            return
        }

        // Now execute the SQL statements.
        if (debug) {
            statements.each { stmt ->
                stmt.asSql().each {
                    println it
                    println()
                }
            }
        }
        else {
            statements.each { stmt ->
//                println "${stmt.class.name} : ${stmt.asSql()}"
                stmt.execute(sql)
            }
        }
    }

    /** Since multiple scripts may be 'included' we need to be able to generate
     *  a new MetaClass for each.
     *
     * @param theClass
     * @param shell
     * @return
     */
    MetaClass getMetaClass(Class<?> theClass, GroovyShell shell) {
        ExpandoMetaClass meta = new ExpandoMetaClass(theClass, false)
        meta.database = { Closure cl ->
            cl.delegate = databaseInfo
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
            // Connect to the database and get the grid, node, and user IDs.
            sql = Sql.newInstance(databaseInfo.fields.url, databaseInfo.fields.username, databaseInfo.fields.password,'org.postgresql.Driver')
            def row = sql.firstRow('select gridid,nodeid,owneruserid from node')
            if (row == null) {
                throw new RuntimeException('The langrid database has not been setup.')
            }
            AbstractDelegate.GRID_ID = row.gridid
            AbstractDelegate.NODE_ID = row.nodeid
            AbstractDelegate.GRID_USER_ID = row.owneruserid
            bindings.setVariable('GRID_ID', row.gridid)
            bindings.setVariable('NODE_ID', row.nodeid)
            bindings.setVariable('GRID_USER_ID', row.owneruserid)
        }

        meta.postgres = { Closure cl ->
            cl.delegate = databaseInfo
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
            // Connect to the database.
            sql = Sql.newInstance(databaseInfo.fields.url, databaseInfo.fields.username, databaseInfo.fields.password,'org.postgresql.Driver')
        }

        meta.domain = { Closure cl ->
            cl.delegate = new DomainDelegate()
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
            statements << cl.delegate
        }

        meta.protocol = { Closure cl ->
            cl.delegate = new ProtocolDelegate()
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
            statements << cl.delegate
        }

        meta.resourceMetaAttribute { Closure cl ->
            cl.delegate = new ResourceMetaAttributeDelegate()
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
            statements << cl.delegate
        }

        meta.resourceType = { Closure cl ->
            cl.delegate = new ResourceTypeDelegate()
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
            statements << cl.delegate
        }

        meta.resource = { Closure cl ->
            cl.delegate = new ResourceDelegate()
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
            statements << cl.delegate
        }

        meta.serviceInterfaceDefinition = { Closure cl ->
            cl.delegate = new ServiceInterfaceDefinitionDelegate()
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
            statements << cl.delegate
        }

        meta.serviceMetaAttribute = { Closure cl ->
            cl.delegate = new ServiceMetaAttributeDelegate()
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
            statements << cl.delegate
        }

        meta.serviceType = { Closure cl ->
            cl.delegate = new ServiceTypeDelegate()
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
            statements << cl.delegate
        }

        meta.service = { Closure cl ->
            cl.delegate = new ServiceDelegate()
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
            statements << cl.delegate
        }

        meta.user = { Closure cl ->
            cl.delegate = new UserDelegate()
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
            statements << cl.delegate
        }

        meta.federate = { Closure cl ->
            cl.delegate = new FederateDelegate()
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
            statements << cl.delegate
        }

        meta.sql = { String statement ->
            statements << new SqlDelegate(statement)
        }

        meta.news = { String content ->
            statements << new NewsDelegate(content)
        }

        meta.comment = { String comment ->
            statements << new CommentDelegate(comment)
        }

        meta.include = { String filename ->
            // Make sure we can find the file. The default behaviour is to
            // look in the same directory as the source script.
            // TODO Allow an absolute path to be specified.

            def filemaker
            if (parentDir != null) {
                filemaker = { String name ->
                    return new File(parentDir, name)
                }
            }
            else {
                filemaker = { String name ->
                    new File(name)
                }
            }

            File file = filemaker(filename)
            if (!file.exists() || file.isDirectory()) {
                // See if there is file with the given name and an lddl
                // extension.
                file = filemaker(filename + ".lddl")
                if (!file.exists()) {
                    throw new FileNotFoundException(filename)
                }
            }
            // Don't include the same file multiple times.
            if (included.contains(filename)) {
                return
            }
            included.add(filename)


            // Parse and run the script.
            Script includedScript = shell.parse(file)
            includedScript.metaClass = getMetaClass(includedScript.class, shell)
            includedScript.run()
        }

        meta.initialize()
        return meta
    }

    static void main(args) {
        if (args.size() == 0) {
            println """
USAGE

java -jar lddl-x.y.z.jar /path/to/script [arg0, arg1, ..., argn]"

"""
            return
        }

        if (args.size() > 1) {
            println args
            if (args[0] == "-debug") {
                println "Enabling debug mode."
                args = args[1..-1]
                debug = true
                AbstractTableDelegate.nextId = 0
            }
        }

        def argv = []
        if (args.size() > 1) {
            // Everything after the script name is passed along as the
            // script's command line arguments.
            argv = args[1..-1]
        }
        new DatabaseDsl().run(new File(args[0]), argv)
    }

}
