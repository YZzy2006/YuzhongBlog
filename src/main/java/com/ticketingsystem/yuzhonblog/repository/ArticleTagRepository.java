package com.ticketingsystem.yuzhonblog.repository;

import com.ticketingsystem.yuzhonblog.entity.ArticleTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ArticleTagRepository extends JpaRepository<ArticleTag, Long> {

    List<ArticleTag> findByArticleId(Long articleId);

    List<ArticleTag> findByTagId(Long tagId);

    @Modifying
    @Query("DELETE FROM ArticleTag at WHERE at.article.id = :articleId")
    void deleteByArticleId(@Param("articleId") Long articleId);

    @Query("SELECT at.tag.id FROM ArticleTag at WHERE at.article.id = :articleId")
    List<Long> findTagIdsByArticleId(@Param("articleId") Long articleId);

    @Query("SELECT at FROM ArticleTag at JOIN FETCH at.tag WHERE at.article.id IN :articleIds")
    List<ArticleTag> findByArticleIdInWithTags(@Param("articleIds") List<Long> articleIds);

    @Query("SELECT COUNT(at) FROM ArticleTag at WHERE at.tag.id = :tagId AND at.article.status = 1")
    long countPublishedByTagId(@Param("tagId") Long tagId);

    @Query("SELECT COUNT(at) FROM ArticleTag at WHERE at.tag.id = :tagId")
    long countByTagId(@Param("tagId") Long tagId);

    @Query("SELECT at.tag.id, COUNT(at) FROM ArticleTag at WHERE at.tag.id IN :tagIds AND at.article.status = 1 GROUP BY at.tag.id")
    List<Object[]> countPublishedByTagIds(@Param("tagIds") List<Long> tagIds);
}
