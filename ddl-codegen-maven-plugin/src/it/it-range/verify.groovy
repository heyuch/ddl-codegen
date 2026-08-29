def a = new File(basedir, 'com/demo/entity/UserA.java')
assert a.isFile() : '范围内表 user_a 应生成 UserA.java'
def b = new File(basedir, 'com/demo/entity/UserB.java')
assert !b.exists() : '范围外表 user_b 不应生成 UserB.java'
