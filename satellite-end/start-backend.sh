#!/bin/bash
cd /demo/sate/demo/satellite-end

# 加载环境变量
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

# 启动应用
java -jar demo/target/springboot-demo-0.0.1-SNAPSHOT.jar
