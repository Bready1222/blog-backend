package com.example.blog.controller; // <--- 【重要】检查包名

import com.example.blog.entity.Article;
import com.example.blog.repository.ArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 1. @RestController 表示这是一个返回 JSON 数据的控制器
// 2. @RequestMapping("/api/articles") 表示所有接口都以这个路径开头
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    // 3. 注入刚才写的 Repository
    @Autowired
    private ArticleRepository articleRepository;

    // --- 接口 1: 获取所有文章 (GET /api/articles) ---
    @GetMapping
    public List<Article> getAllArticles() {
        return articleRepository.findAll();
    }

    // --- 接口 2: 根据 ID 获取单篇文章 (GET /api/articles/1) ---
    @GetMapping("/{id}")
    public Article getArticleById(@PathVariable Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在，ID: " + id));
    }

    // --- 接口 3: 新增一篇文章 (POST /api/articles) ---
    @PostMapping
    public Article createArticle(@RequestBody Article article) {
        // 保存并返回带有新 ID 的文章对象
        return articleRepository.save(article);
    }

    // --- 接口 4: 删除文章 (DELETE /api/articles/1) ---
    @DeleteMapping("/{id}")
    public void deleteArticle(@PathVariable Long id) {
        articleRepository.deleteById(id);
    }
}