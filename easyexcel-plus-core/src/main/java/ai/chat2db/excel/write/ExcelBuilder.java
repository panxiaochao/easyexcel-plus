package ai.chat2db.excel.write;

import java.util.Collection;

import ai.chat2db.excel.context.WriteContext;
import ai.chat2db.excel.write.merge.OnceAbsoluteMergeStrategy;
import ai.chat2db.excel.write.metadata.WriteSheet;
import ai.chat2db.excel.write.metadata.WriteTable;
import ai.chat2db.excel.write.metadata.fill.FillConfig;

/**
 * @author jipengfei
 */
public interface ExcelBuilder {

    /**
     * WorkBook increase value
     *
     * @param data
     *            java basic type or java model extend BaseModel
     * @param writeSheet
     *            Write the sheet
     * @deprecated please use{@link ExcelBuilder#addContent(Collection, WriteSheet, WriteTable)}
     */
    @Deprecated
    void addContent(Collection<?> data, WriteSheet writeSheet);

    /**
     * WorkBook increase value
     *
     * @param data
     *            java basic type or java model extend BaseModel
     * @param writeSheet
     *            Write the sheet
     * @param writeTable
     *            Write the table
     */
    void addContent(Collection<?> data, WriteSheet writeSheet, WriteTable writeTable);

    /**
     * WorkBook fill value
     *
     * @param data
     * @param fillConfig
     * @param writeSheet
     */
    void fill(Object data, FillConfig fillConfig, WriteSheet writeSheet);

    /**
     * Creates new cell range. Indexes are zero-based.
     *
     * @param firstRow
     *            Index of first row
     * @param lastRow
     *            Index of last row (inclusive), must be equal to or larger than {@code firstRow}
     * @param firstCol
     *            Index of first column
     * @param lastCol
     *            Index of last column (inclusive), must be equal to or larger than {@code firstCol}
     * @deprecated please use{@link OnceAbsoluteMergeStrategy}
     */
    @Deprecated
    void merge(int firstRow, int lastRow, int firstCol, int lastCol);

    /**
     * Gets the written data
     *
     * @return
     */
    WriteContext writeContext();

    /**
     * Close io
     *
     * @param onException
     */
    void finish(boolean onException);

}