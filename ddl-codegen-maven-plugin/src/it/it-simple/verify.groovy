def entity = new File(basedir, 'com/demo/entity/User.java')
assert entity.isFile() : 'User.java 应已生成'
def code = new String(entity.bytes, 'UTF-8')
assert code.contains('public class User') : '应生成 public class User'
assert code.contains('private String name') : '应生成 name 字段'
assert code.contains('@Generated') : '成员应带 @Generated'
