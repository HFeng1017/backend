#!/bin/bash
# ============================================================
# 简历平台后端一键部署脚本（适用于宝塔面板 / 任意 Linux 服务器）
# 解决问题：
#   1. JVM 内存未限制 → 与 MySQL/Redis/宝塔抢内存，整机卡死
#   2. 旧进程未杀 → 每次部署进程堆积，内存翻倍
#   3. SQL 日志全量打印 → stdout IO 压力大（已改 Slf4jImpl）
# 用法：
#   ./deploy.sh start    启动（自动先杀旧进程）
#   ./deploy.sh stop     停止
#   ./deploy.sh restart  重启
#   ./deploy.sh status   查看状态
# ============================================================

APP_NAME="resume-platform"
JAR_FILE="platform.jar"
PORT=9090
LOG_DIR="logs"

# JVM 内存限制：小内存服务器（2G）安全值，防止系统 swap 卡死
# 1G 服务器请改为 -Xms128m -Xmx256m -XX:MaxMetaspaceSize=96m
JAVA_OPTS="-Xms256m -Xmx512m -XX:MaxMetaspaceSize=160m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"

mkdir -p "$LOG_DIR"

# 查找旧进程 PID（按端口 + 按 jar 名，双保险）
find_pid() {
    local pid1 pid2
    pid1=$(ss -tlnp 2>/dev/null | grep ":${PORT} " | grep -oP 'pid=\K[0-9]+' | head -1)
    pid2=$(ps -ef | grep "${JAR_FILE}" | grep -v grep | grep -v deploy.sh | awk '{print $2}' | head -1)
    echo "${pid1:-$pid2}"
}

stop() {
    local pid
    pid=$(find_pid)
    if [ -z "$pid" ]; then
        echo "[${APP_NAME}] 没有运行中的进程"
        return 0
    fi
    echo "[${APP_NAME}] 停止旧进程 PID=$pid"
    kill -15 "$pid" 2>/dev/null
    # 最多等待 8 秒优雅退出，超时强杀
    for i in $(seq 1 8); do
        if ! kill -0 "$pid" 2>/dev/null; then
            break
        fi
        sleep 1
    done
    if kill -0 "$pid" 2>/dev/null; then
        echo "[${APP_NAME}] 优雅退出超时，强制结束"
        kill -9 "$pid" 2>/dev/null
        sleep 1
    fi
    echo "[${APP_NAME}] 已停止"
}

start() {
    local pid
    pid=$(find_pid)
    if [ -n "$pid" ]; then
        echo "[${APP_NAME}] 已在运行 PID=$pid，如需重启请执行: ./deploy.sh restart"
        return 0
    fi
    if [ ! -f "$JAR_FILE" ]; then
        echo "[错误] 当前目录找不到 ${JAR_FILE}，请先将 maven 打包产物放到此目录"
        exit 1
    fi
    local log_file="${LOG_DIR}/app-$(date +%Y%m%d).log"
    echo "[${APP_NAME}] 启动中... 日志: ${log_file}"
    nohup java ${JAVA_OPTS} -jar "${JAR_FILE}" > "$log_file" 2>&1 &
    local new_pid=$!
    echo "[${APP_NAME}] 已启动 PID=${new_pid}（JVM 限制: ${JAVA_OPTS}）"
    # 等待端口就绪（最多 40 秒）
    for i in $(seq 1 40); do
        if ss -tln 2>/dev/null | grep -q ":${PORT} "; then
            echo "[${APP_NAME}] 端口 ${PORT} 已就绪 ✓"
            return 0
        fi
        if ! kill -0 "$new_pid" 2>/dev/null; then
            echo "[错误] 进程启动失败，最近日志："
            tail -n 30 "$log_file"
            exit 1
        fi
        sleep 1
    done
    echo "[警告] 40 秒内端口未就绪，请查看日志: tail -f ${log_file}"
}

status() {
    local pid
    pid=$(find_pid)
    if [ -n "$pid" ]; then
        echo "[${APP_NAME}] 运行中 PID=$pid"
        ps -o pid,rss,vsz,etime,cmd -p "$pid"
        echo "（RSS 为实际占用物理内存 KB）"
    else
        echo "[${APP_NAME}] 未运行"
    fi
}

case "$1" in
    start)   start ;;
    stop)    stop ;;
    restart) stop; start ;;
    status)  status ;;
    *)       echo "用法: $0 {start|stop|restart|status}"; exit 1 ;;
esac
