package org.anc.lapps.lddl

import groovy.sql.*
import org.postgresql.largeobject.LargeObject
import org.postgresql.largeobject.LargeObjectManager

import java.sql.Connection
import java.text.SimpleDateFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Base class for delegates that insert values into the database.
 * @author Keith Suderman
 */
abstract class AbstractTableDelegate extends AbstractDelegate {

    abstract String table()
    abstract String columns()
    abstract String values()

    LargeObjectManager manager
    static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS")

    String timestamp() {
        return dateFormat.format(new Date())
    }

    String asSql() {
        validateFields()
        "insert into ${table()} (${columns()}) values (${values()})"
    }

    void execute(Sql sql) {
        sql.execute(asSql())
    }

    int getNext(Sql sql, String table, String column) {
        Integer max = sql.firstRow("select max(${column}) from ${table}".toString())?.values()[0]
        if (max != null) {
            return max + 1
        }
        return 1
    }

    long writeLargeObject(Sql sql, String value) {
        sql.connection.autoCommit = false
        long id = -1
        try {
            getManager(sql.connection)
            id = manager.createLO()
            LargeObject largeObject = manager.open(id)
            largeObject.write(value.bytes)
            largeObject.close()
        }
        finally {
            sql.connection.autoCommit = true
        }
        return id
    }

    long writeZipObject(Sql sql, String name, String value) {
        sql.connection.autoCommit = false
        long id = -1
        try {
            getManager(sql.connection)
            id = manager.createLO()
            LargeObject largeObject = manager.open(id)
            ZipOutputStream zip = new ZipOutputStream(largeObject.outputStream)
            ZipEntry entry = new ZipEntry("${name}.wsdl")
            zip.putNextEntry(entry)
            zip.write(value.bytes)
            zip.closeEntry()
            zip.close()
            largeObject.close()
        }
        finally {
            sql.connection.autoCommit = true
        }
        return id
    }

    synchronized LargeObjectManager getManager(Connection connection) {
        if (manager == null) {
            manager = new LargeObjectManager(connection)
        }
        return manager
    }
}
