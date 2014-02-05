package org.anc.lapps.lddl

/**
 * Base class for delegates that do not touch the database.
 *
 * @author Keith Suderman
 */
abstract class AbstractDelegate {
    // These values will be reinitialized with the correct values pulled
    // from the database.
    static String GRID_ID = 'anc'
    static String NODE_ID = 'picard'
    static String GRID_USER_ID = 'operator1'

    Map fields = [:]
    Set namesCache

    Set fieldNames() {
        return [] as HashSet
    }

    abstract String[] asSql()

    void methodMissing(String name, args) {
//        println "Missing Method: ${name}, ${args}"
        if (fieldNames().contains(name)) {
            if (args.size() == 1) {
                fields[name] = args[0]
            }
            else {
                fields[name] = args
            }
        }
        else {
            throw new MissingPropertyException("${name} is not a valid property name.")
        }
    }

    void setProperty(name,value) {
        throw new MissingPropertyException("Do not use an equal sign when assigning values to properties: ${name}")
    }

    void validateFields() {
        Set names = fieldNames()
        names.each { name ->
            if (fields[name] == null) {
                throw new MissingPropertyException("No value has been specified for ${name}")
            }
        }
        fields.keySet().each { key ->
            if (!names.contains(key)) {
                throw new DelegateException("Unknown field name: ${key}")
            }
        }
    }
}
