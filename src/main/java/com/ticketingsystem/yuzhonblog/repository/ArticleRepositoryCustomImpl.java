package com.ticketingsystem.yuzhonblog.repository;

import com.ticketingsystem.yuzhonblog.entity.Article;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class ArticleRepositoryCustomImpl implements ArticleRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Article> searchPublishedByWords(Integer status, String[] words, Pageable pageable) {
        // Build WHERE clause: OR across all words × all fields
        StringBuilder where = new StringBuilder("a.status = :status AND (");
        for (int i = 0; i < words.length; i++) {
            if (i > 0) where.append(" OR ");
            String param = "w" + i;
            where.append("(a.title LIKE :").append(param)
                 .append(" OR a.summary LIKE :").append(param)
                 .append(" OR a.contentMd LIKE :").append(param).append(")");
        }
        where.append(")");

        // Count query
        TypedQuery<Long> countQuery = em.createQuery(
                "SELECT COUNT(a) FROM Article a WHERE " + where.substring(where.indexOf("AND") + 4), Long.class);
        countQuery.setParameter("status", status);
        for (int i = 0; i < words.length; i++) {
            countQuery.setParameter("w" + i, "%" + words[i] + "%");
        }
        long total = countQuery.getSingleResult();

        // Data query
        TypedQuery<Article> dataQuery = em.createQuery(
                "SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.category WHERE " + where + " ORDER BY a.isTop DESC, a.createdAt DESC",
                Article.class);
        dataQuery.setParameter("status", status);
        for (int i = 0; i < words.length; i++) {
            dataQuery.setParameter("w" + i, "%" + words[i] + "%");
        }
        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());

        List<Article> content = dataQuery.getResultList();
        return new PageImpl<>(content, pageable, total);
    }
}
