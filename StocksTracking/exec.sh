#!/bin/sh
# crontab -e # it sets the cron
# 0 */4 * * * exec.sh # sets the cron for each 4 hours
APP_HOME_DIR=/home/opc/StocksTracking
TELEGRAM_SEND_MESSAGE_JAR=$APP_HOME_DIR/lib/telegramsendmessage-1.4.jar
cd $APP_HOME_DIR && java -cp $TELEGRAM_SEND_MESSAGE_JAR:bin/ Exec
