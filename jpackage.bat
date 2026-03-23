@echo on
REM Скрипт для сборки автономного .exe-файла с помощью jpackage

REM Путь к JavaFX SDK (папка lib должна содержать javafx*.jar)
set FX_LIB=javafx-sdk-21\lib

REM Путь к jar-файлу (предполагается, что он уже собран в IntelliJ)
set INPUT_DIR=out\artifacts\diplom_jar
set JAR_FILE=diplom.jar

REM Папка, куда будет сгенерирован .exe
set OUTPUT_DIR=out\dist

REM Название итогового приложения
set APP_NAME=DiplomApp

REM Запуск jpackage
jpackage ^
  --type exe ^
  --input "%INPUT_DIR%" ^
  --dest "%OUTPUT_DIR%" ^
  --name "%APP_NAME%" ^
  --main-jar "%JAR_FILE%" ^
  --main-class com.simon.diplom.MainApplication ^
  --module-path "%FX_LIB%" ^
  --add-modules javafx.controls,javafx.fxml ^
  --win-dir-chooser ^

echo.
echo ✅ Упаковка завершена. Готовый exe: %OUTPUT_DIR%\%APP_NAME%.exe
pause
