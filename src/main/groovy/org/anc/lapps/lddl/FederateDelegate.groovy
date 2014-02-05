package org.anc.lapps.lddl

/**
 * @author Keith Suderman
 */
class FederateDelegate extends AbstractTableDelegate {

    @Override
    Set fieldNames() {
        return [ 'source', 'target', 'username', 'password', 'url', 'commercial','homepage' ]
    }

    @Override
    String table() {
        return 'federation'
    }

    @Override
    String columns() {
        return 'sourcegridid,targetgridid,sourcegridname,tagetgridname,createddatetime,updateddatetime,targetgriduserid,targetgridaccesstoken,connected,requesting,targetgridhomepageurlclazz,targetgridhomepageurlstringvalue'
    }

    @Override
    String values() {
        def now = timestamp()
        StringBuilder buffer = new StringBuilder()
        String password = encodePassword(fields.password)
        buffer << "'${fields.source}'"
        [fields.target, fields.source,fields.target, now, now, fields.username, password,true,false,'java.net.URL',fields.homepage].each {
            buffer << ",'${it}'"
        }

        return buffer.toString()
    }

    @Override
    String[] asSql() {
        def result = []
        String now = timestamp()
        String columns = 'gridid,createddatetime,updateddatetime,gridname,hosted,operatoruserid,url,autoapproveenabled,commercialuseallowed'
        StringBuilder buffer = new StringBuilder()
        buffer << "'${fields.target}'"
        def commercialUse = fields.commercial ?: false
        [now,now,fields.target,false,fields.username,fields.url,true,commercialUse].each {
            buffer << ", '${it}'"
        }
        result << "insert into grid (${columns}) values (${buffer.toString()})"
        result.addAll(super.asSql())
        return result as String[]
    }
}
