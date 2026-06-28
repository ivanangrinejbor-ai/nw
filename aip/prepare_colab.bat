@echo off
echo Packing aip/ for Colab upload...
cd /d "%~dp0"
powershell -Command "Compress-Archive -Path '.\code_xml_parser.py', '.\tokenizer.py', '.\training_data\projects.json', '.\train_colab.ipynb', '.\requirements.txt' -DestinationPath '.\colab_pack.zip' -Force"
echo Created colab_pack.zip
echo.
echo 1. Upload colab_pack.zip to Google Drive
echo 2. Open https://colab.research.google.com
echo 3. File ^> Upload Notebook ^> train_colab.ipynb (из zip)
echo 4. Mount Drive, распаковать zip, запустить все ячейки
echo 5. Скачать model.tflite, vocab.json, model_metadata.json
echo 6. Положить их в catroid\src\main\assets\
pause
