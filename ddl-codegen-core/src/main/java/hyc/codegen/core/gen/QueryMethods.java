package hyc.codegen.core.gen;

import java.util.ArrayList;
import java.util.List;

import hyc.codegen.core.model.Index;
import hyc.codegen.core.naming.NamingService;

/**
 * 索引 → 查询方法的拆分规则（DESIGN §12）：按最左前缀逐级拆分；
 * 唯一键且参数=全部列 → 单值返回，其余（前缀/普通索引）→ 列表返回。
 */
public final class QueryMethods {

    private QueryMethods() {
        throw new AssertionError("no instances");
    }

    /** 索引 → 前缀规格（1..n 列，逐级拆分）。 */
    public static List<Spec> of(Index index, NamingService naming) {
        List<Spec> specs = new ArrayList<>();
        List<String> columns = index.getColumns();
        for (int i = 1; i <= columns.size(); i++) {
            List<String> prefix = new ArrayList<>(columns.subList(0, i));
            boolean uniqueFull = index.isUnique() && i == columns.size();
            specs.add(new Spec(naming.indexMethodName(prefix), prefix, uniqueFull));
        }
        return specs;
    }

    /** 单个查询方法规格。 */
    public static final class Spec {

        private final String methodName;

        private final List<String> columns;

        private final boolean uniqueFull;

        Spec(String methodName, List<String> columns, boolean uniqueFull) {
            this.methodName = methodName;
            this.columns = columns;
            this.uniqueFull = uniqueFull;
        }

        public String getMethodName() {
            return methodName;
        }

        public List<String> getColumns() {
            return columns;
        }

        /** 唯一键且覆盖全部索引列 → 最多一条（@Nullable 单值返回）。 */
        public boolean isUniqueFull() {
            return uniqueFull;
        }

    }

}
