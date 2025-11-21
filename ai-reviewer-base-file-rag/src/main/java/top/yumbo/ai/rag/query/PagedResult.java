package top.yumbo.ai.rag.query;

import lombok.Getter;
import top.yumbo.ai.rag.model.SearchResult;

/**
 * 分页结果模型
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Getter
public class PagedResult extends SearchResult {

    private final int currentPage;
    private final int pageSize;
    private final int totalPages;
    private final boolean hasNext;
    private final boolean hasPrevious;

    public PagedResult(SearchResult result, int currentPage, int pageSize) {
        // 复制SearchResult的数据
        this.getDocuments().addAll(result.getDocuments());
        this.setTotalHits(result.getTotalHits());
        this.setQueryTimeMs(result.getQueryTimeMs());
        this.setQuery(result.getQuery());
        this.setHasMore(result.isHasMore());
        this.setPage(result.getPage());
        this.setPageSize(result.getPageSize());

        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalPages = (int) Math.ceil((double) result.getTotalHits() / pageSize);
        this.hasNext = currentPage < totalPages - 1;
        this.hasPrevious = currentPage > 0;
    }
}

