package org.saintqd.cspsolver;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.saintqd.cspsolver.utils.CollectionUtils;
import org.saintqd.cspsolver.utils.SheetUtils;

import java.awt.*;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class CSPSolver {

    public static void main(String[] args) {

        Path decodedPath;
        try {
            URL url = CSPSolver.class.getProtectionDomain().getCodeSource().getLocation();
            decodedPath = Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        String fileLocation = "";

        Frame frame = new Frame();
        FileDialog fileDialog = new FileDialog(frame,"Открыть файл");
        fileDialog.setDirectory(decodedPath.toAbsolutePath().toString());
        fileDialog.setVisible(true);

        String selectedFileName = fileDialog.getFile();
        String selectedDirectory = fileDialog.getDirectory();

        fileDialog.dispose();
        frame.dispose();

        if (selectedFileName != null && selectedDirectory != null) {
            fileLocation = Paths.get(selectedDirectory,selectedFileName).toString();
            System.out.println("Выбранный файл: " + fileLocation);
        }
        else {
            System.out.println("Выбор файла отменён.");
        }
        if (fileLocation.isEmpty()) {
            System.exit(0);
            return;
        }

        Path pathToFile = Paths.get(fileLocation);

        Workbook workbook;
        try (FileInputStream file = new FileInputStream(fileLocation)) {
            workbook = new HSSFWorkbook(file);
        } catch (IOException e) {
            System.out.println("Excel-файл не найден!");
            System.out.println("Указанный путь: "+fileLocation);
            System.out.println("Проверьте корректность введённого пути к файлу.");
            throw new RuntimeException(e);
        }

        Map<Integer, List<Integer>> valuesToRows = new HashMap<>();

        Sheet sheet = workbook.getSheetAt(0);
        int[] selectedColumns = {3,5};
        for (int column : selectedColumns) {
            Iterator<Row> rowIterator = sheet.rowIterator();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (row.getRowNum() < 3)
                    continue;
                Cell cell = row.getCell(column);
                if (cell != null && cell.getCellType() != CellType.BLANK) {
                    CellType cellType = cell.getCellType();
                    int value = cellType == CellType.NUMERIC
                            ? (int) cell.getNumericCellValue()
                            : Integer.parseInt(cell.getStringCellValue().replaceAll("\\D", ""));
                    List<Integer> rows = valuesToRows.getOrDefault(value, new ArrayList<>());
                    rows.add(row.getRowNum());
                    valuesToRows.put(value, rows);
                }
            }
        }

        StringBuilder dataToWrite = new StringBuilder();
        int totalSize = 0, totalWaste = 0;
        selectedColumns = new int[]{9, 10};
        Map<Integer,List<Pair<Integer,Integer>>> preparedSets = new HashMap<>();
        for (int value : valuesToRows.keySet()) {
            System.out.println("\nРамка: " + value);
            List<Pair<Integer, Integer>> setForValue = preparedSets.getOrDefault(value, new ArrayList<>());

            Map<Integer, Integer> blocksList = new HashMap<>();
            for (int column : selectedColumns) {
                for (int rowNum : valuesToRows.get(value)) {
                    Cell cell = sheet.getRow(rowNum).getCell(column);
                    CellType cellType = cell.getCellType();
                    int cellValue = cellType == CellType.NUMERIC
                            ? (int) cell.getNumericCellValue()
                            : Integer.parseInt(cell.getStringCellValue().replaceAll("\\D", ""));
                    Cell quantityCell = sheet.getRow(rowNum).getCell(1);
                    CellType quantityCellType = quantityCell.getCellType();
                    int quantity = quantityCellType == CellType.NUMERIC
                            ? (int) quantityCell.getNumericCellValue()
                            : Integer.parseInt(quantityCell.getStringCellValue().replaceAll("\\D", ""));
                    blocksList.put(cellValue, blocksList.getOrDefault(cellValue, 0) + quantity);

                    for (int amount = 0; amount < quantity; amount++)
                        setForValue.add(Pair.of(cellValue, rowNum - 2));
                }
            }
            preparedSets.put(value, setForValue);

            List<Map<Integer, Integer>> subBlocksList = CollectionUtils.splitMapBySize(blocksList, 24);
            for (Map<Integer, Integer> selectedMapPart : subBlocksList) {
                int[] blocks = selectedMapPart.keySet().stream().mapToInt(i -> i).toArray();
                int[] quantities = selectedMapPart.values().stream().mapToInt(i -> i).toArray();
                System.out.println(Arrays.toString(blocks));
                System.out.println(Arrays.toString(quantities));
                int i = 0, max_size = 6000;
                CuttingStock cuttingStock = new CuttingStock(max_size, blocks, quantities);

                while (cuttingStock.hasMoreCombinations()) {
                    System.out.println("\nЗаготовка # " + (++i));
                    totalSize += max_size;
                    List<Pair<Integer, Integer>> map = cuttingStock.nextCombination();
                    int waste = max_size;
                    for (Pair<Integer, Integer> entry : map) {
                        int key = entry.getLeft();
                        int intValue = entry.getRight();
                        System.out.println(key + "  *  " + intValue);
                        waste -= key * intValue;

                        for (int amount = 0; amount < intValue; amount++) {
                            setForValue = preparedSets.getOrDefault(value, new ArrayList<>());
                            Iterator<Pair<Integer, Integer>> setIterator = setForValue.iterator();
                            while (setIterator.hasNext()) {
                                Pair<Integer, Integer> pair = setIterator.next();
                                if (pair.getLeft() == key) {
                                    dataToWrite.append(key).append(" рамка ").append(value).append(" ячейка ").append(pair.getRight()).append("\n");
                                    setIterator.remove();
                                    break;
                                }
                            }
                            preparedSets.put(value, setForValue);
                        }
                    }
                    System.out.println("Остаток: " + waste);
                    totalWaste += waste;
                }
            }
        }

        try {
            String filename = FilenameUtils.removeExtension(pathToFile.getFileName().toString()).split("\\s+")[0];
            Path outputPath = Paths.get(filename+".txt");
            Files.write(outputPath, dataToWrite.toString().getBytes(Charset.forName("Windows-1251")));
        } catch (IOException e) {
            System.err.println("Возникла ошибка при записи в файл: " + e.getMessage());
            throw new RuntimeException(e);
        }

        double efficiency = 1.0 - (double) totalWaste / totalSize;
        System.out.println("\nОбщая длина: "+totalSize);
        System.out.println("Общий остаток: "+totalWaste);
        System.out.println("Коэффициент эффективности: "+efficiency);


        // Создание отформатированного Excel-файла
        workbook = null;
        Path outputPath = null;
        try {
            String unparsedOutputFilename = pathToFile.getFileName().toString();
            String outputFilename = FilenameUtils.removeExtension(unparsedOutputFilename);
            String finalName = unparsedOutputFilename.replace(outputFilename,outputFilename+" печатная форма");
            outputPath = Files.copy(pathToFile,Paths.get(finalName),StandardCopyOption.REPLACE_EXISTING);
            FileInputStream file = new FileInputStream(outputPath.toString());
            workbook = new HSSFWorkbook(file);
        } catch (IOException e) {
            System.out.println("Не удалось создать копию Excel-файла!");
            throw new RuntimeException(e);
        }

        sheet = workbook.getSheetAt(0);

        SheetUtils.unmergeAllCells(sheet);

        SheetUtils.deleteColumn(sheet,7);
        SheetUtils.deleteColumn(sheet,7);

        for (int i = 0; i < 10; i++) {
            SheetUtils.deleteColumn(sheet,9);
        }

        for (int i = 0; i < 21; i++) {
            SheetUtils.deleteColumn(sheet,10);
        }

        SheetUtils.deleteColumn(sheet,12);

        for (int i = 0; i < 16; i++) {
            SheetUtils.deleteColumn(sheet,14);
        }

        sheet.setColumnWidth(0,5*256 + 128);
        sheet.setColumnWidth(1,4*256 + 169);
        sheet.setColumnWidth(2,5*256 + 128 + 82);
        sheet.setColumnWidth(3,10*256 + 128 - 41);
        sheet.setColumnWidth(4,6*256 + 41);
        sheet.setColumnWidth(5,10*256 + 128 - 41);
        sheet.setColumnWidth(6,5*256 + 128 + 82);
        sheet.setColumnWidth(7,10*256 + 128 - 41);
        sheet.setColumnWidth(8,10*256 + 128 - 41);
        sheet.setColumnWidth(9,10*256 + 128 - 41);
        sheet.setColumnWidth(10,17*256 + 128);
        sheet.setColumnWidth(11,10*256 + 128 - 41);
        sheet.setColumnWidth(12,10*256 + 128 - 41);
        sheet.setColumnWidth(13,10*256 + 128 - 41);

        selectedColumns = new int[]{11};
        for (int column : selectedColumns) {
            Iterator<Row> rowIterator = sheet.rowIterator();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Cell noteCell = row.getCell(column);
                if (row.getRowNum() > 1 && noteCell != null && noteCell.getCellType() == CellType.STRING
                        && noteCell.getStringCellValue() != null && !noteCell.getStringCellValue().trim().isEmpty()) {
                    sheet.autoSizeColumn(column);
                    break;
                }
            }
        }

        sheet.addMergedRegion(new CellRangeAddress(0,1,0,0));
        sheet.addMergedRegion(new CellRangeAddress(0,1,1,1));
        sheet.addMergedRegion(new CellRangeAddress(0,0,2,6));
        sheet.addMergedRegion(new CellRangeAddress(0,0,7,8));
        sheet.addMergedRegion(new CellRangeAddress(0,1,9,9));
        sheet.addMergedRegion(new CellRangeAddress(0,0,10,13));

        try (FileOutputStream outputStream = new FileOutputStream(outputPath.toString())) {
            workbook.write(outputStream);
        } catch (IOException e) {
            System.out.println("Не удалось сохранить копию Excel-файла!");
            throw new RuntimeException(e);
        }

        System.exit(0);
        return;
    }
}
