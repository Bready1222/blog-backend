package com.example.blog.entity; // <--- 【重要】检查这里是否变红？如果是，改成你项目的真实包名

import jakarta.persistence.*; // 引入 JPA 注解
import java.time.LocalDateTime;

// 1. 告诉 Spring 这是一个实体类，对应数据库的一张表
@Entity
@Table(name = "articles") // 表名叫 articles
public class Article {

    // 2. 主键 ID，自动增长
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 3. 标题，不能为空，最长200字符
    @Column(nullable = false, length = 200)
    private String title;

    // 4. 内容，可以是长文本
    @Column(columnDefinition = "TEXT")
    private String content;

    // 5. 创建时间
    @Column(updatable = false)
    private LocalDateTime createTime;

    // 构造函数：新建对象时自动设置当前时间
    public Article() {
        this.createTime = LocalDateTime.now();
    }

    // --- 下面是 Getter 和 Setter (必须要有，否则无法读写数据) ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}