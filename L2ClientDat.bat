@echo off
@color 0A
title L2ClientDat
:start
echo %DATE% %TIME%
echo.
java -Xms512m -Xmx2048m -cp ./data/lib/*;L2ClientDat.jar l2god.Boot
pause