package com.example.blog.repository; // <--- 确保包名和你实际的一致

import com.example.blog.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    // 空着就行，不用写代码
}