package hyc.codegen.tree;

import javax.annotation.processing.Generated;

/**
 * 测试枚举
 * <p>
 * 测试 comment body
 *
 * @author humpy
 * @see <a href="https://baidu.com">百度</a>
 * @since 2025-12-17
 * @version 2025-12-29 第一版本
 */
public class Demo {

    /**
     * ID
     */
    private Integer id;

    /**
     * 名称
     */
    private String name;

    /**
     * 状态
     */
    private Status status;

    /**
     * 无参构造方法
     */
    public Demo() {
    }

    /**
     * 全参构造方法
     *
     * @param id ID
     * @param name 名称
     * @param status 状态
     */
    public Demo(Integer id, String name, Status status) {
        this.id = id;
        this.name = name;
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public enum Status {

        /**
         * 初始状态
         * <p>
         * 状态 1
         */
        @Generated("test")
        INIT("init"),

        OK("ok"),

        ;

        /**
         * 状态编码
         */
        private final String code;

        /**
         * 构造函数
         *
         * @param code 状态编码
         */
        Status(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

    }

}
