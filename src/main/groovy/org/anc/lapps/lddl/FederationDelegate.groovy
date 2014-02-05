package org.anc.lapps.lddl

/**
 * @author Keith Suderman
 */
class FederationDelegate extends AbstractTableDelegate {

    @Override
    Set fieldNames() {
        return [ 'source', 'target', 'username', 'password', 'url' ]
    }

    @Override
    String table() {
        return 'federation'
    }

    @Override
    String columns() {
        return 'sourcegridid,targetgridid,sourcegridname,createddatetime,updateddatetime,targetgriduserid,targetgridaccesstoken'
    }

    @Override
    String values() {
        def now = timestamp()
        StringBuilder buffer = new StringBuilder()
        String password = encodePassword(fields.password)
        buffer << "'${fields.source}'"
        [fields.target, fields.source, now, now, fields.username, password].each {
            buffer << ",'${it}'"
        }

        return buffer.toString()
    }

    @Override
    String[] asSql() {
        def result = []
        String now = timestamp()
        String columns = 'gridid,createddatetime,updateddatetime,gridname,hosted,operatoruserid,url'
        StringBuilder buffer = new StringBuilder()
        buffer << "'${fields.target}'"
        [now,now,fields.target,false,fields.username,fields.url].each {
            buffer << ", '${it}'"
        }
        result << "insert into grid (${columns}) values (${buffer.toString()})"
        result.addAll(super.asSql())
        return result as String[]
    }
}
