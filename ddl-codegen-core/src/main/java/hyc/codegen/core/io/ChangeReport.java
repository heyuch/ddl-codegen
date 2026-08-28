package hyc.codegen.core.io;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * 执行变更报告：逐文件变更摘要，供 CLI 输出与验收。
 */
public final class ChangeReport {

    private final List<Entry> entries = new ArrayList<>();

    private final List<String> warnings = new ArrayList<>();

    public void add(Path path, ChangeStatus status) {
        add(path, status, null);
    }

    public void add(Path path, ChangeStatus status, @Nullable String detail) {
        entries.add(new Entry(path, status, detail));
    }

    /** 变更条目（防御性拷贝）。 */
    public List<Entry> getEntries() {
        return new ArrayList<>(entries);
    }

    /** 记一条警告（不中断）。 */
    public void addWarning(String message) {
        warnings.add(message);
    }

    /** 本次执行的警告（防御性拷贝）。 */
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    /** 是否存在真实变更（排除 UNCHANGED）。 */
    public boolean hasChanges() {
        for (Entry entry : entries) {
            if (entry.getStatus() != ChangeStatus.UNCHANGED) {
                return true;
            }
        }
        return false;
    }

    /** 汇总文本（如 {@code +3 ~1 -1 =2}：新建/更新/删除/无变化）。 */
    public String summary() {
        int created = 0;
        int updated = 0;
        int deleted = 0;
        int unchanged = 0;
        for (Entry entry : entries) {
            switch (entry.getStatus()) {
                case CREATED:
                    created++;
                    break;
                case UPDATED:
                    updated++;
                    break;
                case DELETED:
                    deleted++;
                    break;
                default:
                    unchanged++;
                    break;
            }
        }
        return "+" + created + " ~" + updated + " -" + deleted + " =" + unchanged;
    }

    /** 单文件变更条目。 */
    public static final class Entry {

        private final Path path;

        private final ChangeStatus status;

        @Nullable
        private final String detail;

        Entry(Path path, ChangeStatus status, @Nullable String detail) {
            this.path = path;
            this.status = status;
            this.detail = detail;
        }

        public Path getPath() {
            return path;
        }

        public ChangeStatus getStatus() {
            return status;
        }

        @Nullable
        public String getDetail() {
            return detail;
        }

    }

}
