package edu.hbmd.taotao.qingjia.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 公共分页返回体（前后端分离接口统一用这个，不要再直接把 MyBatis-Plus 的 Page 丢给前端）。
 *
 * 为什么不用 Page：
 * 1. Page 序列化出来会带上 orders / optimizeCountSql / searchCount / countId / maxLimit
 *    这些纯内部字段，前端一个都用不上，白白增大响应体。
 * 2. Page 的字段类型和语义会随 MyBatis-Plus 版本变化，直接暴露给前端等于把升级风险
 *    转嫁给 Vue 那边。这里固定成 5 个字段，就是一个稳定契约。
 *
 * 对应前端 src/type/page.ts 里的 PageResult<T>。
 */
@Data
public class PageResult<T> {

    /** 当前页数据 */
    private List<T> records = Collections.emptyList();
    /** 总记录数 */
    private long total;
    /** 当前页码，从 1 开始 */
    private long current;
    /** 每页条数 */
    private long size;
    /** 总页数 */
    private long pages;

    public PageResult() {
    }

    public PageResult(List<T> records, long total, long current, long size, long pages) {
        this.records = records == null ? Collections.emptyList() : records;
        this.total = total;
        this.current = current;
        this.size = size;
        this.pages = pages;
    }

    /**
     * 由 MyBatis-Plus 的分页结果直接转换。
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        if (page == null) {
            return empty(1, 10);
        }
        return new PageResult<>(page.getRecords(), page.getTotal(),
                page.getCurrent(), page.getSize(), page.getPages());
    }

    /**
     * 分页元信息取自 page，但数据换成转换后的 records。
     * 用于 mapper 查出的是 Pojo、返回给前端要转成 Vo 的场景。
     */
    public static <T> PageResult<T> of(IPage<?> page, List<T> records) {
        if (page == null) {
            return new PageResult<>(records, records == null ? 0 : records.size(), 1, records == null ? 0 : records.size(), 1);
        }
        return new PageResult<>(records, page.getTotal(),
                page.getCurrent(), page.getSize(), page.getPages());
    }

    /**
     * 空结果。查询条件命中 0 条时用，保证前端拿到的结构永远一致，
     * 不用写 records 可能为 null 的防御代码。
     */
    public static <T> PageResult<T> empty(long current, long size) {
        return new PageResult<>(Collections.emptyList(), 0, current, size, 0);
    }
}
