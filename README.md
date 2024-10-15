# ai_manager
## 准备工作
1. 安装java、maven、mysql server
2. mvn clean install 
3. 初始化mysql
    * 新建数据库
    * 参考[application.yml](src%2Fmain%2Fresources%2Fapplication.yml)文件，按需修改db连接配置
4. 启动应用
    * java -jar target/ai-manager.jar
    * 启动过程中会自动执行schema.sql,新建数据表
