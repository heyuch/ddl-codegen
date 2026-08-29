def entity = new File(basedir, 'com/demo/entity/User.java')
assert entity.isFile() : '内联 DDL 应生成 User.java'
def code = new String(entity.bytes, 'UTF-8')
assert code.contains('private String name') : '应生成 name 字段'
