# Git Push Skill

当用户要求上传代码到 GitHub（包括"上传"、"push"、"commit"、"提交到github"等指令）时，自动执行以下规则：

## 规则

1. **按功能模块分 commit** — 不同模块的改动分开提交，commit message 说明是哪个模块
2. **禁止上传 .md 文件** — CLAUDE.md、README.md、agents.md、docs/*.md 等全部排除
3. **禁止上传配置文件** — application.properties 等包含敏感信息的配置文件排除
4. **禁止上传日志文件**

## 执行步骤

1. `git status --short` 检查所有改动
2. 按模块分类文件（订单、出库、退货、快递、库存、商品等）
3. 每个模块单独 `git add` + `git commit`
4. 只添加代码文件（.java、.xml、.vue、.ts、.tsx、.sql 等）
5. 跳过所有 .md 文件和 .properties 文件
6. `git push origin <branch>`
7. 推送完成后展示 commit 列表确认
