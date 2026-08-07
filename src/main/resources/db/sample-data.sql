-- 样例动态数据
-- 手动执行: mysql -u root -p yuzhong_blog < src/main/resources/db/sample-data.sql

INSERT INTO timeline_entry (title, description, cover_image, entry_date, link_url, category, sort_order, status, mood, tags, images, view_count, like_count, created_at, updated_at) VALUES
('终于把博客部署上线了', '折腾了整整一周，从 Spring Boot 后端到 Vue 3 前端，再到 Nginx 反向代理和 SSL 证书，总算把个人博客部署到了服务器上。第一篇动态，记录一下这个时刻。', 'https://picsum.photos/seed/blog-deploy/600/400', '2026-07-01', NULL, '技术', 10, 'PUBLISHED', '🎉', '["博客","部署","Spring Boot"]', NULL, 128, 23, NOW(), NOW()),

('Vue 3 的 Composition API 真香', '从 Options API 迁移到 Composition API 之后，代码组织清晰多了。特别是 setup 语法糖，配合 ref 和 computed，写起来特别顺手。推荐还在犹豫的同学赶紧切换。', 'https://picsum.photos/seed/vue3-composition/600/400', '2026-07-05', NULL, '技术', 8, 'PUBLISHED', '💡', '["Vue3","前端","Composition API"]', NULL, 95, 18, NOW(), NOW()),

('周末去爬山了', '难得周末天气好，和朋友去爬了白云山。山顶的风景特别好，拍了不少照片。下山的时候腿都在抖，看来平时运动太少了。', 'https://picsum.photos/seed/mountain-hike/600/400', '2026-07-06', NULL, '生活', 5, 'PUBLISHED', '⛰️', '["爬山","周末","运动"]', '["https://picsum.photos/seed/mountain-1/400/400","https://picsum.photos/seed/mountain-2/400/400","https://picsum.photos/seed/mountain-3/400/400"]', 67, 12, NOW(), NOW()),

('解决了一个困扰三天的 Bug', 'JPA 的 N+1 问题真的防不胜胜。一个简单的列表查询，因为关联对象的懒加载，导致页面加载了 200 多条 SQL。用 fetch join 一句搞定，性能直接提升 10 倍。', 'https://picsum.photos/seed/debug-jpa/600/400', '2026-07-08', NULL, '技术', 9, 'PUBLISHED', '🐛', '["JPA","性能优化","Bug"]', NULL, 156, 31, NOW(), NOW()),

('新入手了一副降噪耳机', 'Sony WH-1000XM5，戴上之后世界瞬间安静了。在咖啡厅写代码的时候特别好用，周围的嘈杂声完全听不到。音质也很不错，听播客和音乐都很舒服。', 'https://picsum.photos/seed/sony-headphones/600/400', '2026-07-10', NULL, '数码', 6, 'PUBLISHED', '🎧', '["耳机","Sony","数码好物"]', NULL, 82, 15, NOW(), NOW()),

('给博客加了 AI 聊天助手', '接入了 Claude API，做了一个智能助手组件。用户可以和 AI 对话，还能搜索文章。流式输出的效果很酷，打字机一样的体验。', 'https://picsum.photos/seed/ai-chatbot/600/400', '2026-07-12', NULL, '技术', 10, 'PUBLISHED', '🤖', '["AI","Claude","聊天机器人"]', NULL, 203, 42, NOW(), NOW()),

('深夜写代码的快乐', '凌晨两点，安静的房间里只有键盘敲击声。这种专注的感觉太棒了，白天总是被各种消息打断，只有深夜才是真正属于自己的时间。今晚把动态模块的点赞功能做完了。', 'https://picsum.photos/seed/night-coding/600/400', '2026-07-14', NULL, '随想', 3, 'PUBLISHED', '🌙', '["深夜","编程","随想"]', NULL, 89, 20, NOW(), NOW()),

('学了一天 Rust', '被 Rust 的所有权机制折磨了一整天，编译器一直在报错。不过写通之后确实能感受到它的精妙之处，内存安全不是说说而已。等有空要把一个 CLI 工具用 Rust 重写。', 'https://picsum.photos/seed/rust-lang/600/400', '2026-07-15', NULL, '技术', 7, 'PUBLISHED', '🦀', '["Rust","学习","编程语言"]', NULL, 134, 28, NOW(), NOW()),

('今天天气真好', '蓝天白云，微风习习。中午出去散了个步，买了一杯冰美式。回到工位继续改需求，心情莫名地好。有时候快乐就是这么简单。', 'https://picsum.photos/seed/sunny-day/600/400', '2026-07-16', NULL, '生活', 2, 'PUBLISHED', '☀️', '["天气","日常","咖啡"]', '["https://picsum.photos/seed/sunny-1/400/400","https://picsum.photos/seed/sunny-2/400/400"]', 45, 8, NOW(), NOW()),

('前端性能优化笔记', '整理了一份前端性能优化清单：1. 图片懒加载 2. 路由懒加载 3. 减少 backdrop-filter 使用 4. 虚拟滚动长列表 5. 防抖节流。每一条都是实战中踩过的坑。', 'https://picsum.photos/seed/frontend-perf/600/400', '2026-07-17', NULL, '技术', 9, 'PUBLISHED', '📝', '["前端","性能优化","笔记"]', NULL, 178, 35, NOW(), NOW());
