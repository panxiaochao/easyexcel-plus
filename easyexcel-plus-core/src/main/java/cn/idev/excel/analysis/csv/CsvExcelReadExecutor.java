package cn.idev.excel.analysis.csv;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.idev.excel.analysis.ExcelReadExecutor;
import cn.idev.excel.enums.ByteOrderMarkEnum;
import cn.idev.excel.enums.CellDataTypeEnum;
import cn.idev.excel.enums.RowTypeEnum;
import cn.idev.excel.exception.ExcelAnalysisException;
import cn.idev.excel.exception.ExcelAnalysisStopSheetException;
import cn.idev.excel.metadata.Cell;
import cn.idev.excel.metadata.data.ReadCellData;
import cn.idev.excel.read.metadata.ReadSheet;
import cn.idev.excel.read.metadata.holder.ReadRowHolder;
import cn.idev.excel.read.metadata.holder.csv.CsvReadWorkbookHolder;
import cn.idev.excel.util.SheetUtils;
import cn.idev.excel.util.StringUtils;
import cn.idev.excel.context.csv.CsvReadContext;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;

/**
 * read executor
 *
 * @author zhuangjiaju
 */
@Slf4j
public class CsvExcelReadExecutor implements ExcelReadExecutor {

    private final List<ReadSheet> sheetList;
    private final CsvReadContext csvReadContext;

    public CsvExcelReadExecutor(CsvReadContext csvReadContext) {
        this.csvReadContext = csvReadContext;
        sheetList = new ArrayList<>();
        ReadSheet readSheet = new ReadSheet();
        sheetList.add(readSheet);
        readSheet.setSheetNo(0);
    }

    @Override
    public List<ReadSheet> sheetList() {
        return sheetList;
    }

    @Override
    public void execute() {
        CSVParser csvParser;
        try {
            csvParser = csvParser();
            csvReadContext.csvReadWorkbookHolder().setCsvParser(csvParser);
        } catch (IOException e) {
            throw new ExcelAnalysisException(e);
        }
        for (ReadSheet readSheet : sheetList) {
            readSheet = SheetUtils.match(readSheet, csvReadContext);
            if (readSheet == null) {
                continue;
            }
            try {
                csvReadContext.currentSheet(readSheet);

                int rowIndex = 0;

                for (CSVRecord record : csvParser) {
                    dealRecord(record, rowIndex++);
                }
            } catch (ExcelAnalysisStopSheetException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Custom stop!", e);
                }
            }

            // The last sheet is read
            csvReadContext.analysisEventProcessor().endSheet(csvReadContext);
        }
    }

    private CSVParser csvParser() throws IOException {
        CsvReadWorkbookHolder csvReadWorkbookHolder = csvReadContext.csvReadWorkbookHolder();
        CSVFormat csvFormat = csvReadWorkbookHolder.getCsvFormat();
        ByteOrderMarkEnum byteOrderMark = ByteOrderMarkEnum.valueOfByCharsetName(
            csvReadContext.csvReadWorkbookHolder().getCharset().name());
        if (csvReadWorkbookHolder.getMandatoryUseInputStream()) {
            return buildCsvParser(csvFormat, csvReadWorkbookHolder.getInputStream(), byteOrderMark);
        }
        if (csvReadWorkbookHolder.getFile() != null) {
            return buildCsvParser(csvFormat, Files.newInputStream(csvReadWorkbookHolder.getFile().toPath()),
                byteOrderMark);
        }
        return buildCsvParser(csvFormat, csvReadWorkbookHolder.getInputStream(), byteOrderMark);
    }

    private CSVParser buildCsvParser(CSVFormat csvFormat, InputStream inputStream, ByteOrderMarkEnum byteOrderMark)
        throws IOException {
        if (byteOrderMark == null) {
            return csvFormat.parse(
                new InputStreamReader(inputStream, csvReadContext.csvReadWorkbookHolder().getCharset()));
        }
        return csvFormat.parse(new InputStreamReader(new BOMInputStream(inputStream, byteOrderMark.getByteOrderMark()),
            csvReadContext.csvReadWorkbookHolder().getCharset()));
    }

    private void dealRecord(CSVRecord record, int rowIndex) {
        Map<Integer, Cell> cellMap = new LinkedHashMap<>();
        Iterator<String> cellIterator = record.iterator();
        int columnIndex = 0;
        Boolean autoTrim = csvReadContext.currentReadHolder().globalConfiguration().getAutoTrim();
        while (cellIterator.hasNext()) {
            String cellString = cellIterator.next();
            ReadCellData<String> readCellData = new ReadCellData<>();
            readCellData.setRowIndex(rowIndex);
            readCellData.setColumnIndex(columnIndex);

            // csv is an empty string of whether <code>,,</code> is read or <code>,"",</code>
            if (StringUtils.isNotBlank(cellString)) {
                readCellData.setType(CellDataTypeEnum.STRING);
                readCellData.setStringValue(autoTrim ? cellString.trim() : cellString);
            } else {
                readCellData.setType(CellDataTypeEnum.EMPTY);
            }
            cellMap.put(columnIndex++, readCellData);
        }

        RowTypeEnum rowType = MapUtils.isEmpty(cellMap) ? RowTypeEnum.EMPTY : RowTypeEnum.DATA;
        ReadRowHolder readRowHolder = new ReadRowHolder(rowIndex, rowType,
            csvReadContext.readWorkbookHolder().getGlobalConfiguration(), cellMap);
        csvReadContext.readRowHolder(readRowHolder);

        csvReadContext.csvReadSheetHolder().setCellMap(cellMap);
        csvReadContext.csvReadSheetHolder().setRowIndex(rowIndex);
        csvReadContext.analysisEventProcessor().endRow(csvReadContext);
    }
}
