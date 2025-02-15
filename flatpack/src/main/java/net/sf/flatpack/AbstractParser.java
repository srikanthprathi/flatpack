/*
 * ObjectLab, http://www.objectlab.co.uk/open is supporting FlatPack.
 *
 * Based in London, we are world leaders in the design and development
 * of bespoke applications for the securities financing markets.
 *
 * <a href="http://www.objectlab.co.uk/open">Click here to learn more</a>
 *           ___  _     _           _   _          _
 *          / _ \| |__ (_) ___  ___| |_| |    __ _| |__
 *         | | | | '_ \| |/ _ \/ __| __| |   / _` | '_ \
 *         | |_| | |_) | |  __/ (__| |_| |__| (_| | |_) |
 *          \___/|_.__// |\___|\___|\__|_____\__,_|_.__/
 *                   |__/
 *
 *                     www.ObjectLab.co.uk
 *
 * $Id: ColorProvider.java 74 2006-10-24 22:19:05Z benoitx $
 *
 * Copyright 2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.sf.flatpack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import net.sf.flatpack.structure.ColumnMetaData;
import net.sf.flatpack.util.ParserUtils;
import net.sf.flatpack.xml.MetaData;

/**
 * @author xhensevb
 * @author zepernick
 *
 */
public abstract class AbstractParser implements Parser {

    private boolean handlingShortLines = false;

    private boolean ignoreExtraColumns = false;

    private boolean preserveLeadingWhitespace = true; // this is the old default value

    private boolean preserveTrailingWhitespace = false;

    private boolean columnNamesCaseSensitive = false;

    private boolean initialised = false;

    private boolean ignoreParseWarnings = false;

    private boolean nullEmptyStrings = false;

    /** Map of column metadata's */
    private MetaData pzMetaData = null;

    private String dataDefinition = null;

    private Reader dataSourceReader = null;

    private List<Reader> readersToClose = null;

    private boolean flagEmptyRows;

    private boolean storeRawDataToDataError;

    private boolean storeRawDataToDataSet;

    private String dataFileTable = "DATAFILE";

    private String dataStructureTable = "DATASTRUCTURE";

    private boolean addSuffixToDuplicateColumnNames = false;

    public boolean isAddSuffixToDuplicateColumnNames() {
        return addSuffixToDuplicateColumnNames;
    }

    @Override
    public Parser setAddSuffixToDuplicateColumnNames(final boolean addSuffixToDuplicateColumnNames) {
        this.addSuffixToDuplicateColumnNames = addSuffixToDuplicateColumnNames;
        return this;
    }

    protected AbstractParser(final Reader dataSourceReader) {
        this.dataSourceReader = dataSourceReader;
    }

    protected AbstractParser(final Reader dataSourceReader, final String dataDefinition) {
        this.dataSourceReader = dataSourceReader;
        this.dataDefinition = dataDefinition;
    }

    protected void initStreamOrSource(final InputStream dataSourceStream, final File dataSource) throws FileNotFoundException {
        if (dataSourceStream != null) {
            final Reader r = new InputStreamReader(dataSourceStream);
            setDataSourceReader(r);
            addToCloseReaderList(r);
        } else if (dataSource != null) {
            final Reader r = new FileReader(dataSource);
            setDataSourceReader(r);
            addToCloseReaderList(r);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.flatpack.PZParser#isHandlingShortLines()
     */
    @Override
    public boolean isHandlingShortLines() {
        return handlingShortLines;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.flatpack.PZParser#setHandlingShortLines(boolean)
     */
    @Override
    public Parser setHandlingShortLines(final boolean handleShortLines) {
        this.handlingShortLines = handleShortLines;
        return this;
    }

    @Override
    public boolean isIgnoreExtraColumns() {
        return ignoreExtraColumns;
    }

    @Override
    public Parser setIgnoreExtraColumns(final boolean ignoreExtraColumns) {
        this.ignoreExtraColumns = ignoreExtraColumns;
        return this;
    }

    @Override
    public boolean isPreserveLeadingWhitespace() {
        return preserveLeadingWhitespace;
    }

    @Override
    public Parser setPreserveLeadingWhitespace(final boolean preserveLeadingWhitespace) {
        this.preserveLeadingWhitespace = preserveLeadingWhitespace;
        return this;
    }

    @Override
    public boolean isPreserveTrailingWhitespace() {
        return preserveTrailingWhitespace;
    }

    @Override
    public Parser setPreserveTrailingWhitespace(final boolean preserveTrailingWhitespace) {
        this.preserveTrailingWhitespace = preserveTrailingWhitespace;
        return this;
    }

    @Override
    public final DataSet parse() {
        if (!initialised) {
            init();
        }
        return doParse();
    }

    @Override
    public final StreamingDataSet parseAsStream() {
        return new StreamingRecord(parse());
    }

    @Override
    public final Stream<Record> stream() {
        return new StreamingRecord(parse()).stream();
    }

    protected abstract DataSet doParse();

    protected abstract void init();

    protected void closeReaders() throws IOException {
        if (readersToClose != null) {
            for (final Reader r : readersToClose) {
                r.close();
            }
        }
    }

    // adds a reader to the close list. the list will be processed after parsing
    // is completed.
    protected void addToCloseReaderList(final Reader r) {
        if (readersToClose == null) {
            readersToClose = new ArrayList<>();
        }
        readersToClose.add(r);
    }

    protected void addToMetaData(final List<ColumnMetaData> columns) {
        if (pzMetaData == null) {
            pzMetaData = new MetaData(columns, ParserUtils.buidColumnIndexMap(columns, this));
        } else {
            pzMetaData.setColumnsNames(columns);
            pzMetaData.setColumnIndexMap(ParserUtils.buidColumnIndexMap(columns, this));
        }
    }

    protected boolean isInitialised() {
        return initialised;
    }

    protected void setInitialised(final boolean initialised) {
        this.initialised = initialised;
    }

    protected String getDataDefinition() {
        return dataDefinition;
    }

    protected void setDataDefinition(final String dataDefinition) {
        this.dataDefinition = dataDefinition;
    }

    /**
     * Adds a new error to this DataSet. These can be collected, and retrieved
     * after processing
     *
     * @param ds
     *            the data set from the parser
     * @param errorDesc
     *            String description of error
     * @param lineNo
     *            line number error occurred on
     * @param errorLevel
     *            errorLevel 1,2,3 1=warning 2=error 3= severe error'
     * @param lineData
     *            Data of the line which failed the parse
     */
    protected void addError(final DefaultDataSet ds, final String errorDesc, final int lineNo, final int errorLevel, final String lineData) {
        if (errorLevel == 1 && isIgnoreParseWarnings()) {
            // user has selected to not log warnings in the parser
            return;
        }
        ds.addError(new DataError(errorDesc, lineNo, errorLevel, lineData));
    }

    /**
     * Adds a new error to this DataSet. These can be collected, and retrieved
     * after processing
     *
     * @param ds
     *            the data set from the parser
     * @param errorDesc
     *            String description of error
     * @param lineNo
     *            line number error occurred on
     * @param errorLevel
     *            errorLevel 1,2,3 1=warning 2=error 3= severe error'
     * @param lineData
     *            Data of the line which failed the parse
     * @param lastColName
     *            Column name which was the last one parsed successfully (in case of too few col)
     * @param lastColValue
     *            value of the last Column
     */
    protected void addError(final DefaultDataSet ds, final String errorDesc, final int lineNo, final int errorLevel, final String lineData,
            final String lastColName, final String lastColValue) {
        if (errorLevel == 1 && isIgnoreParseWarnings()) {
            // user has selected to not log warnings in the parser
            return;
        }
        ds.addError(new DataError(errorDesc, lineNo, errorLevel, lineData, lastColName, lastColValue));
    }

    /**
     * @return the dataSourceReader
     */
    protected Reader getDataSourceReader() {
        return dataSourceReader;
    }

    /**
     * @param dataSourceReader
     *            the dataSourceReader to set
     */
    protected void setDataSourceReader(final Reader dataSourceReader) {
        this.dataSourceReader = dataSourceReader;
    }

    @Override
    public boolean isColumnNamesCaseSensitive() {
        return columnNamesCaseSensitive;
    }

    @Override
    public Parser setColumnNamesCaseSensitive(final boolean columnNamesCaseSensitive) {
        this.columnNamesCaseSensitive = columnNamesCaseSensitive;
        return this;
    }

    @Override
    public boolean isIgnoreParseWarnings() {
        return ignoreParseWarnings;
    }

    @Override
    public Parser setIgnoreParseWarnings(final boolean ignoreParseWarnings) {
        this.ignoreParseWarnings = ignoreParseWarnings;
        return this;
    }

    @Override
    public boolean isNullEmptyStrings() {
        return nullEmptyStrings;
    }

    @Override
    public Parser setNullEmptyStrings(final boolean nullEmptyStrings) {
        this.nullEmptyStrings = nullEmptyStrings;
        return this;
    }

    public MetaData getPzMetaData() {
        return pzMetaData;
    }

    public void setPzMetaData(final MetaData pzMap) {
        this.pzMetaData = pzMap;
    }

    /**
     * @return the flagEmptyRows
     */
    @Override
    public boolean isFlagEmptyRows() {
        return flagEmptyRows;
    }

    /**
     * @param flagEmptyRows
     *            the flagEmptyRows to set
     */
    @Override
    public Parser setFlagEmptyRows(final boolean flagEmptyRows) {
        this.flagEmptyRows = flagEmptyRows;
        return this;
    }

    /**
     * @return the storeRawDataToDataError
     */
    @Override
    public boolean isStoreRawDataToDataError() {
        return storeRawDataToDataError;
    }

    /**
     * @param storeRawDataToDataError
     *            the storeRawDataToDataError to set
     */
    @Override
    public Parser setStoreRawDataToDataError(final boolean storeRawDataToDataError) {
        this.storeRawDataToDataError = storeRawDataToDataError;
        return this;
    }

    /**
     * @return the storeRawDataToDataSet
     */
    @Override
    public boolean isStoreRawDataToDataSet() {
        return storeRawDataToDataSet;
    }

    /**
     * @param storeRawDataToDataSet
     *            the storeRawDataToDataSet to set
     */
    @Override
    public Parser setStoreRawDataToDataSet(final boolean storeRawDataToDataSet) {
        this.storeRawDataToDataSet = storeRawDataToDataSet;
        return this;
    }

    @Override
    public String getDataFileTable() {
        return dataFileTable;
    }

    @Override
    public Parser setDataFileTable(final String dataFileTable) {
        this.dataFileTable = dataFileTable;
        return this;
    }

    @Override
    public String getDataStructureTable() {
        return dataStructureTable;
    }

    @Override
    public Parser setDataStructureTable(final String dataStructureTable) {
        this.dataStructureTable = dataStructureTable;
        return this;
    }
}
