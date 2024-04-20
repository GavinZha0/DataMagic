package com.ninestar.datapie.datamagic.service;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.basicdataset.cv.classification.Mnist;
import ai.djl.basicmodelzoo.basic.Mlp;
import ai.djl.engine.Engine;
import ai.djl.inference.Predictor;
import ai.djl.metric.Metric;
import ai.djl.metric.Metrics;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.transform.CenterCrop;
import ai.djl.modality.cv.transform.Normalize;
import ai.djl.modality.cv.transform.Resize;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.modality.cv.translator.ImageClassificationTranslator;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.Trainer;
import ai.djl.training.dataset.Batch;
import ai.djl.training.dataset.Dataset;
import ai.djl.training.dataset.RandomAccessDataset;
import ai.djl.training.evaluator.Accuracy;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.loss.Loss;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.*;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.text.csv.*;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.googlecode.aviator.AviatorEvaluator;
import com.ninestar.datapie.datamagic.bridge.ImporterUploadReqType;
import com.ninestar.datapie.datamagic.bridge.MlTrainEpochIndType;
import com.ninestar.datapie.datamagic.entity.*;
import com.ninestar.datapie.datamagic.repository.*;
import com.ninestar.datapie.datamagic.utils.DbUtils;
import com.ninestar.datapie.datamagic.utils.IpUtils;
import com.ninestar.datapie.framework.consts.MysqlColumnType;
import com.ninestar.datapie.framework.model.ColumnField;
import com.ninestar.datapie.framework.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.split.FileSplit;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static ai.djl.training.EasyTrain.*;

@Service
@Slf4j
public class AsyncTaskService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String FILE_SERVER = System.getProperty("user.dir") + "/fileServer";

    @Resource
    WebStompService stompService;

    @Resource
    private LogAccessRepository accRepository;

    @Resource
    private LogActionRepository actRepository;

    @Resource
    private DataSourceRepository sourceRepository;

    @Resource
    private GisIpRepository gisIpRepository;

    @Resource
    private DataImportRepository importRepository;

    @Resource
    private DbUtils dbUtils;

    private Integer numEpoch = 1;
    private Integer pctProgress = 0;

    @Async
    public void executeAccLog(LogAccessEntity log) {
        // login and logout will be recorded here
        List<LogAccessEntity> history = accRepository.findByIpAndLocationNotNull(log.getIp());
        if(history.size()>0){
            // get location from history
            log.setLocation(history.get(0).getLocation());
        }
        else{
            if(true){
                // get location from location DB
                Long ipValue = IpUtils.ipToLong(log.getIp());
                GisIpEntity ipLoc = gisIpRepository.findByIpFromLessThanEqualAndIpToGreaterThanEqual(ipValue, ipValue);
                log.setLocation(ipLoc.getCountry());
            }
            else{
                // get location from IP based db file
                // Chinese word (no!!!)
                log.setLocation(IpUtils.getLocByIP(log.getIp()));
            }
        }

        //save into DB
        accRepository.save(log);
    }

    @Async
    public void executeActLog(LogActionEntity log) {

        //save into DB
        actRepository.save(log);
    }

    @Async
    public void importFileTask(Map<String, InputStream> fileMap, String fileDir, ImporterUploadReqType config, DataImportEntity record) throws Exception {
        // upload files to ftp and get status
        Map<String, String> fileStatus =  uploadFilesToFtp(fileMap, fileDir, config);
        record.setStatus("waiting"); // wait for ETL
        importRepository.save(record);

        // check datasource
        if (!dbUtils.isSourceExist(config.source)) {
            DataSourceEntity source = sourceRepository.findById(config.source).get();
            String pwd = new String(Base64.decode(source.getPassword()));
            dbUtils.add(source.getId(), source.getName(), source.getType(), source.getUrl(), source.getParams(), source.getUsername(), pwd);
        }

        // start ETL
        record.setStatus("processing");
        importRepository.save(record);
        Integer importedRecords = 0;
        record.setStatus("success");

        try {
            // create a new table if table doesn't exist
            // remove all records if overwrite is set to true
            Boolean ready = prepareDbTable(config);

            if(config.type.equalsIgnoreCase("CSV")){
                importedRecords = importCsvToDb(config, fileStatus);
            } else if(config.type.equalsIgnoreCase("JSON")){
                importedRecords = importCsvToDb(config, fileStatus);
            } else if(config.type.equalsIgnoreCase("XLSX")){
                importedRecords = importCsvToDb(config, fileStatus);
            } else {
                logger.error("Unsupported file type to import");
                record.setStatus("error");
                record.setDetail("Unsupported file type");
                return;
            }
        } catch (Exception e){
            record.setStatus("error");
            record.setDetail(e.getMessage().substring(0, 254));
        } finally {
            record.setRecords(importedRecords);
            importRepository.save(record);
        }
    }

    // upload file to ftp and return status
    private Map<String, String> uploadFilesToFtp(Map<String, InputStream> fileMap, String fileDir, ImporterUploadReqType config){
        Map<String, String> fileStatus = new HashMap<>();

        // create destination folder
        File targetFolder = new File(fileDir);
        if (!targetFolder.exists()) {
            if(!targetFolder.mkdirs()){
                return fileStatus;
            }
        }

        String fullPathFile = "";
        for(String fullName : fileMap.keySet()){
            try {
                // read stream and write to destination file
                fullPathFile = targetFolder + "/" + fullName;
                BufferedReader fileReader = new BufferedReader(new InputStreamReader(fileMap.get(fullName), Charset.forName(config.fileAttrs.encoding)));
                BufferedWriter fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fullPathFile), Charset.forName(config.fileAttrs.encoding)));

                // save file to local FTP
                char[] b = new char[5120];
                int len = 0;
                while ((len = fileReader.read(b)) != -1) {
                    fileWriter.write(b, 0, len);
                }
                fileReader.close();
                fileWriter.close();
                fileStatus.put(fullPathFile, "success");

            }catch (Exception e){
                fileStatus.put(fullPathFile, "failure");
                logger.error(e.getMessage());
            }
        }

        return fileStatus;
    }

    private Boolean prepareDbTable(ImporterUploadReqType config) throws Exception {
        // confirm table
        if(!dbUtils.isTableIsExist(config.source, config.table)){
            // create a new table for imported files
            List<ColumnField> cols = new ArrayList<>();
            List<Object[]> result = new ArrayList<>();
            String sql = "CREATE TABLE $TABLE_NAME$ ( uid BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, $FIELDS$) ENGINE = InnoDB, COMMENT = '$COMMENT$';";
            sql = sql.replace("$TABLE_NAME$", config.table);
            String fields = "";

            // enrich type if precision is not null
            for(ImporterUploadReqType.ColFieldType item: config.colFields){
                if(item.ignore==null || !item.ignore){
                    String colType = "VARCHAR(255)"; // default
                    if(item.type.equalsIgnoreCase("VARCHAR")){
                        if(StrUtil.isNotEmpty(item.precision)){
                            colType = "VARCHAR(" + item.precision + ")";
                        }
                    } else {
                        colType = item.type;
                    }
                    if(colType.equalsIgnoreCase("TIMESTAMP")){
                        // allow empty (it doesn't allow empty without this setting, for MySql)
                        colType += " NULL";
                    }

                    fields += item.alias + " " + colType + " COMMENT '" + item.title + "', ";
                }
            }
            fields = fields.substring(0, fields.length()-2);
            sql = sql.replace("$FIELDS$", fields);
            sql = sql.replace("$COMMENT$", config.fileNames.get(0).name);

            try {
                dbUtils.execute(config.source, sql);
            } catch (Exception e) {
                logger.error(e.getMessage());
                return false;
            }
        }
        else if(config.overwrite!=null && config.overwrite){
            //table exist but we want to remove all records first
            String sql = "TRUNCATE TABLE $TABLE_NAME$;";
            sql = sql.replace("$TABLE_NAME$", config.table);
            try {
                dbUtils.execute(config.source, sql);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(e.getMessage());
                return false;
            }
        }
        return true;
    }

    private CsvRow csvTransform(CsvRow row, ImporterUploadReqType config, Map<String, Integer> headerMap, List<ImporterUploadReqType.ColFieldType> colFields) {
        CsvRow finalRow = new CsvRow(0, headerMap, new ArrayList<>());
        ImporterUploadReqType.DbConfigType dbConfig = config.dbConfig;
        String numRegex ="[^(0-9).]";

        for(ImporterUploadReqType.ColFieldType field: colFields) {
            // key is original index
            String value = row.get(field.key).trim();

            // check if it is in null map
            if(StrUtil.isEmpty(value)){
                finalRow.add(null);
                continue;
            } else if(dbConfig.nullMap.size()>0){
                if(dbConfig.nullMap.contains(value)){
                    finalRow.add(null);
                    continue;
                }
            }

            try {
                if(field.type.toUpperCase().startsWith("VARCHAR")){
                    // limit string length
                    Integer len = 255; // default length
                    if(StrUtil.isNotEmpty(field.precision)){
                        len = Integer.parseInt(field.precision);
                    }
                    if(value.length()>len){
                        value = value.substring(0, len);
                    }
                } else if(field.type.equalsIgnoreCase("INTEGER") || field.type.equalsIgnoreCase("BIGINT")
                        || field.type.equalsIgnoreCase("FLOAT") || field.type.equalsIgnoreCase("DOUBLE")) {
                    // only keep number and dot ('%', '$' and ',' will be removed)
                    value = Pattern.compile(numRegex).matcher(value).replaceAll("").trim();

                    if(StrUtil.isNotEmpty(field.formula)){
                        // run the formula
                        String expr = field.formula.replaceAll("value", value);
                        value = AviatorEvaluator.execute(expr).toString();
                        //final Dict dict = Dict.create().set("value", Double.parseDouble(value));
                        //value = ExpressionUtil.eval(field.formula, dict).toString();
                    }

                    // precision
                    if(StrUtil.isNotEmpty(field.precision) && value.contains(".")){
                        // format value (%.2f)
                        value = String.format("%."+field.precision+"f", Double.parseDouble(value));
                    }
                } else if(field.type.equalsIgnoreCase("BOOLEAN")){
                    if(dbConfig.trueMap.size()>0){
                        if(dbConfig.trueMap.contains(value.toLowerCase())){
                            // covert to TRUE
                            value = "true";
                        }
                    }

                    if(dbConfig.falseMap.size()>0){
                        if(dbConfig.falseMap.contains(value.toLowerCase())){
                            // convert to FALSE
                            value = "false";
                        }
                    }
                } else if(field.type.equalsIgnoreCase("TIME")){
                    String tsFormat = DateUtils.tsFormatDetect(value, config.fileAttrs.tsFormat);
                    LocalTime localTime = LocalTime.parse(value, DateTimeFormatter.ofPattern(tsFormat));
                    // convert to standard date format

                    // convert it to standard format (yyyy-MM-dd)
                    DateTimeFormatter standardFormat = DateTimeFormatter.ofPattern(DatePattern.NORM_TIME_PATTERN);
                    value = standardFormat.format(localTime);
                } else if(field.type.equalsIgnoreCase("DATE")){
                    String tsFormat = DateUtils.tsFormatDetect(value, config.fileAttrs.tsFormat);
                    LocalDate localDate = LocalDate.parse(value, DateTimeFormatter.ofPattern(tsFormat));
                    // convert to standard date format

                    // convert it to standard format (yyyy-MM-dd)
                    DateTimeFormatter standardFormat = DateTimeFormatter.ofPattern(DatePattern.NORM_DATE_PATTERN);
                    value = standardFormat.format(localDate);
                } else if(field.type.equalsIgnoreCase("DATETIME") || field.type.equalsIgnoreCase("TIMESTAMP")){
                    String tsFormat = DateUtils.tsFormatDetect(value, config.fileAttrs.tsFormat);

                    // "03/03/2023 10:00:00" is regarded as UTC time. Hutool CAN'T parse with specific time zone
                    // Hutool, detect common ts format automatically
                    //date = DateUtil.parse(value);
                    // Hutool, parse datetime then CONVERT to specific timezone (like 'Asia/Shanghai')
                    //date = DateUtil.parse(value, tsFormat).setTimeZone(TimeZone.getTimeZone(ZoneId.of("Asia/Shanghai")));

                    ZonedDateTime zonedDateTime;
                    if(tsFormat.endsWith("X")){
                        // "yyyy-MM-dd'T'HH:mm:ssX", ISO 8601, with timezone;  "yyyy-MM-dd HH:mm:ssX", RFC 3339, with timezone
                        if(value.endsWith("Z") || value.substring(value.length()-3).indexOf("+")==0 || value.substring(value.length()-3).indexOf("-")==0){
                            // 2023-03-15T06:30:15Z; 2023-03-15T06:30:15+08
                            zonedDateTime = ZonedDateTime.parse(value, DateTimeFormatter.ofPattern(tsFormat));
                        } else if(value.substring(value.length()-5).indexOf("+")==0 || tsFormat.substring(value.length()-5).indexOf("+")==0) {
                            // 2023-03-15T06:30:15+0800
                            zonedDateTime = ZonedDateTime.parse(value, DateTimeFormatter.ofPattern(tsFormat+"X"));
                        } else if(value.substring(value.length()-3).indexOf(":")==0) {
                            // 2023-03-15T06:30:15+08:00
                            zonedDateTime = ZonedDateTime.parse(value, DateTimeFormatter.ofPattern(tsFormat+"XX"));
                        } else {
                            return null;
                        }
                    }  else  if(tsFormat.endsWith("z") || tsFormat.endsWith("O") || tsFormat.endsWith("]")){
                        // 2015-05-05 10:15:30 PDT;; 2015-05-05 10:15:30 Europe/Paris, RFC 822, with timezone
                        // "2023-03-15, 06:30:15+08:00 [Asia/Shanghai]"
                        zonedDateTime = ZonedDateTime.parse(value, DateTimeFormatter.ofPattern(tsFormat));
                    } else {
                        // datetime string doesn't contain timezone, but we specific it
                        // "03/03/2023 10:00:00", "2023-03-15T06:30:15", timezone="Asia/Shanghai"
                        // pass timestamp string WITH specific format and timezone
                        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(tsFormat).withZone(ZoneId.of(config.fileAttrs.timezone));
                        zonedDateTime = ZonedDateTime.parse(value, dateTimeFormatter);
                        // zonedDateTime = "2023-03-01T10:00+08:00[Asia/Shanghai]", ISO_ZONED_DATE_TIME
                    }

                    // convert it to local time (UTC, system default) with standard format (yyyy-MM-dd HH:mm:ss)
                    DateTimeFormatter standardFormat = DateTimeFormatter.ofPattern(DatePattern.NORM_DATETIME_PATTERN);
                    value = standardFormat.withZone(ZoneId.systemDefault()).format(zonedDateTime);
                    // value = "2023-03-01 02:00:00", local UTC time
                }
            } catch (Exception e){
                logger.error(e.getMessage());
                return null;
            }

            // add valid value to new row (no ignored column)
            finalRow.add(value);
        }

        return finalRow;
    }

    private Integer importCsvToDb(ImporterUploadReqType config, Map<String, String> fileStatus) throws Exception {
        dbUtils.setProgress(0);
        String sqlTemplate = "INSERT INTO $TABLE_NAME$($FIELDS$) VALUES($VALUES$)";

        String fields = "";
        String placeholders = "";
        final List<String> fieldList = new ArrayList<>();
        final Map<String, String> headerType = new LinkedHashMap<>();
        final Map<String, Integer> headerMap = new LinkedHashMap<>(fieldList.size());
        List<String> placeholderList = new ArrayList<String>();
        List<ImporterUploadReqType.ColFieldType> dbColFields = new ArrayList<>();

        // build fields and header
        Integer newIdx = 0;
        for(int i=0; i<config.colFields.size(); i++){
            ImporterUploadReqType.ColFieldType colCfg = config.colFields.get(i);
            colCfg.javaType = MysqlColumnType.toJavaType(colCfg.type);
            if(colCfg.ignore == null || !colCfg.ignore){
                if(colCfg.type.equalsIgnoreCase("VARCHAR")){
                    if(StrUtil.isNotEmpty(colCfg.precision)){
                        colCfg.type = "VARCHAR(" + colCfg.precision + ")";
                    } else {
                        colCfg.type = "Varchar(255)";
                    }
                }

                dbColFields.add(colCfg);
                fieldList.add(colCfg.alias); // column name
                headerMap.put(colCfg.alias, newIdx); // column name and new index
                headerType.put(colCfg.alias, colCfg.type); // column name and type
                placeholderList.add("?"); // placeholder of value
                newIdx++;
            }
        }

        // build sql to insert records
        fields = String.join(",", fieldList);
        placeholders = String.join(",", placeholderList);
        sqlTemplate = sqlTemplate.replace("$TABLE_NAME$", config.table);
        sqlTemplate = sqlTemplate.replace("$FIELDS$", fields);
        sqlTemplate = sqlTemplate.replace("$VALUES$", placeholders);
        AtomicReference<Integer> inportedRecords = new AtomicReference<>(0);

        // load file one by one
        for(String fullFileName : fileStatus.keySet()){
            if(fileStatus.get(fullFileName).equalsIgnoreCase("FAILURE")){
                // skip failure file which was not uploaded to FTP
                continue;
            }

            // Csv read config
            CsvReadConfig csvReadConfig = new CsvReadConfig();
            csvReadConfig.setContainsHeader(config.fileAttrs.header);
            csvReadConfig.setFieldSeparator(config.fileAttrs.delimiter.charAt(0));
            csvReadConfig.setTextDelimiter(config.fileAttrs.quote.charAt(0));
            csvReadConfig.setSkipEmptyRows(true);
            try {
                // transfer file to temp file
                File csvFile = new File(fullFileName);

                // read csv file as stream
                CsvReader csvReader = new CsvReader(new InputStreamReader(new FileInputStream(csvFile)), csvReadConfig);
                Stream<CsvRow> csvRows = csvReader.stream();
                List<CsvRow> csvRowList = new ArrayList<>();
                Integer batchSize = 2000; // can be changed
                String finalSqlTemplate = sqlTemplate;
                csvRows.forEach((row)->{
                    // transform data row by row
                    CsvRow csvRow = csvTransform(row, config, headerMap, dbColFields);
                    if(csvRow!=null){
                        // transformed successfully
                        csvRowList.add(csvRow);
                    }
                    if(csvRowList.size()>=batchSize){
                        try {
                            // run sql to insert data
                            Boolean succ = dbUtils.executeBatch(config.source, finalSqlTemplate, dbColFields, csvRowList);
                            if(succ){
                                inportedRecords.updateAndGet(v -> v + csvRowList.size());
                                logger.info("importCsvToDb: " + inportedRecords.get() + " records have been uploaded");
                            }
                        } catch (Exception e) {
                            logger.error(e.getMessage());
                        } finally {
                            // clean up for next batch
                            csvRowList.clear();
                        }
                    }
                });

                if(csvRowList.size()>0) {
                    // run sql to insert data
                    Boolean succ = dbUtils.executeBatch(config.source, finalSqlTemplate, dbColFields, csvRowList);
                    if(succ){
                        inportedRecords.updateAndGet(v -> v + csvRowList.size());
                    }
                }
            }catch (Exception e){
                logger.error(e.getMessage());
            }
        }
        return inportedRecords.get();
    }

    @Async
    public CompletableFuture<Integer> executeDl4j(String outPath, String msgTarget) throws IOException {
        // log start with message target
        // log will be forwarded to UDP and save target
        logger.info("TARGET>>>"+msgTarget);
        JSONObject msgChat = new JSONObject();

        File targetFolder = new File(outPath);
        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }

        int height = 28; // 图片像素高
        int width = 28;// 图片像素宽
        int nChannels = 1; // Number of input channels
        int outputNum = 10; // The number of possible outcomes
        int batchSize = 64; // Test batch size
        int nEpochs = 1; // Number of training epochs
        Random randNumGen = new Random(123);

        String BASE_PATH = FILE_SERVER + "/public/data/mnist";
        if (!new File(BASE_PATH).exists()) {
            logger.info("数据集文件不存在，{}", BASE_PATH);
            msgChat.set("logs", "数据集文件不存在\n");
            msgChat.set("status", 0);
            stompService.p2pMessage(msgTarget, null, msgChat.toString());
            return CompletableFuture.completedFuture(1);
        }

        // 标签生成器，将指定文件的父目录作为标签
        ParentPathLabelGenerator labelMaker = new ParentPathLabelGenerator();//以父级目录名作为分类的标签名
        // 归一化配置(把像素值区间 0-255 压缩到0-1 区间)
        DataNormalization imageScaler = new ImagePreProcessingScaler();

        logger.info("Load data....");
        //读取图片，数据格式为NCHW
        File trainData = new File( BASE_PATH + "/training");
        FileSplit trainSplit = new FileSplit(trainData, NativeImageLoader.ALLOWED_FORMATS, randNumGen);
        ImageRecordReader trainRR = new ImageRecordReader(height, width, nChannels, labelMaker);//构造图片读取类
        trainRR.initialize(trainSplit);
        DataSetIterator trainIter = new RecordReaderDataSetIterator(trainRR, batchSize, 1, outputNum);
        // 归一化
        imageScaler.fit(trainIter);
        trainIter.setPreProcessor(imageScaler);

        //读取测试数据
        File testData = new File(BASE_PATH + "/testing");
        FileSplit testSplit = new FileSplit(testData, NativeImageLoader.ALLOWED_FORMATS, randNumGen);
        ImageRecordReader testRR = new ImageRecordReader(height, width, nChannels, labelMaker);
        testRR.initialize(testSplit);
        DataSetIterator testIter = new RecordReaderDataSetIterator(testRR, batchSize, 1, outputNum);
        testIter.setPreProcessor(imageScaler);

        /*
            Construct the neural network
         */
        logger.info("Build model....");
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .weightInit(WeightInit.XAVIER)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Nesterovs(0.01,0.9))
                .list()
                .layer(0, new ConvolutionLayer.Builder(5, 5)
                        //nIn and nOut specify depth. nIn here is the nChannels and nOut is the number of filters to be applied
                        .nIn(nChannels)
                        .stride(1, 1)
                        .nOut(20)
                        .activation(Activation.IDENTITY)
                        .build())
                .layer(1, new SubsamplingLayer.Builder(PoolingType.MAX)
                        .kernelSize(2,2)
                        .stride(2,2)
                        .build())
                .layer(2, new ConvolutionLayer.Builder(5, 5)
                        //Note that nIn need not be specified in later layers
                        .stride(1, 1)
                        .nOut(50)
                        .activation(Activation.IDENTITY)
                        .build())
                .layer(3, new SubsamplingLayer.Builder(PoolingType.MAX)
                        .kernelSize(2,2)
                        .stride(2,2)
                        .build())
                .layer(4, new DenseLayer.Builder().activation(Activation.SIGMOID)
                        .nOut(500).build())
                .layer(5, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .nOut(outputNum)
                        .activation(Activation.SOFTMAX)
                        .build())
                .setInputType(InputType.convolutionalFlat(height,width,1)) //See note below
                .build();


        // 根据配置初始化神经网络模型
        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();

/*
        // early stop
        //在epoch终止条件最大为30epoch、最大为20分钟的训练时间的情况下，计算每个epoch的得分，并将中间结果保存到磁盘
        EarlyStoppingConfiguration esConf = new EarlyStoppingConfiguration.Builder()
                .epochTerminationConditions(new MaxEpochsTerminationCondition(30))
                .iterationTerminationConditions(new MaxTimeIterationTerminationCondition(20, TimeUnit.MINUTES))
                .scoreCalculator(new DataSetLossCalculator(testIter, true))
                .evaluateEveryNEpochs(1)
                .modelSaver(new LocalFileModelSaver(new File(outPath + "/model.zip")))
                .build();


        EarlyStoppingTrainer modelTrainer = new EarlyStoppingTrainer(esConf,conf,trainIter);
        // 早停法配置好之后开始训练
        EarlyStoppingResult<MultiLayerNetwork> result = modelTrainer.fit();
        model = result.getBestModel();
*/


        logger.info("Train model....");
        // 保存统计信息
        //StatsStorage statsStorageF = new FileStatsStorage(new File(outPath, "stats.dl4j"));
        //添加StatsListener来在网络定型时收集这些信息，损失函数值
        //model.setListeners(new StatsListener(statsStorageF), new ScoreIterationListener(30));
        model.setListeners(new ScoreIterationListener(30));

        // doesn't work
        // 初始化用户界面后端
        //UIServer uiServer = UIServer.getInstance();
        //uiServer.enableRemoteListener(); //启用远程支持, how to use it to forward state to a UI server
        //设置网络信息（随时间变化的梯度、分值等）的存储位置。这里将其存储于内存。
        //StatsStorage statsStorageM = new InMemoryStatsStorage();
        //将StatsStorage实例连接至用户界面，让StatsStorage的内容能够被可视化
        //uiServer.attach(statsStorageM);
        //StatsStorageRouter remoteUIRouter = new RemoteUIStatsStorageRouter("http://localhost:9000");
        //model.setListeners(new StatsListener(remoteUIRouter), new ScoreIterationListener(10));


        for( int epoch=0; epoch<nEpochs; epoch++ ) {
            MlTrainEpochIndType trainInd = new MlTrainEpochIndType();
            trainInd.stage = "train";
            trainInd.epoch = epoch;
            trainInd.numEpoch = nEpochs;
            trainInd.iterator = 0;
            trainInd.numIterator = Math.toIntExact(trainSplit.length())/batchSize;

            msgChat.clear();
            msgChat.set("trainInd", trainInd);
            stompService.p2pMessage(msgTarget, null, msgChat.toString());
            model.fit(trainIter);

            trainInd.progress = 100;
            msgChat.clear();
            msgChat.set("trainInd", trainInd);
            stompService.p2pMessage(msgTarget, null, msgChat.toString());

            logger.info("Evaluate model....");
            trainInd.stage = "eval";
            trainInd.progress = 10;
            msgChat.clear();
            msgChat.set("trainInd", trainInd);
            stompService.p2pMessage(msgTarget, null, msgChat.toString());

            Evaluation eval = model.evaluate(testIter);

            trainInd.progress = 100;
            trainInd.accuracy = (float) eval.accuracy();
            trainInd.precision = (float) eval.precision();
            trainInd.recall = (float) eval.recall();
            trainInd.f1 = (float) eval.f1();
            msgChat.clear();
            msgChat.set("trainInd", trainInd);
            stompService.p2pMessage(msgTarget, null, msgChat.toString());

            logger.info(eval.confusionToString());
            String aaa = eval.getConfusionMatrix().toHTML();
            String bbb = eval.getConfusionMatrix().toCSV();

            trainIter.reset();
            testIter.reset();
        }

        // 保存模型
        File modelFile = new File(outPath + "/DL4J_model.zip");
        ModelSerializer.writeModel(model, modelFile, true);

        msgChat.clear();
        msgChat.set("status", 0);
        stompService.p2pMessage(msgTarget, null, msgChat.toString());
        logger.info("TARGET<<<"+msgTarget);
        return CompletableFuture.completedFuture(0);
    }


    @Async
    public CompletableFuture<Integer>  executeDJL(String outPath, String msgTarget) throws IOException {
        Marker TARGETMARKER = MarkerFactory.getMarker(msgTarget);
        logger.info("TARGET>>>"+msgTarget);

        JSONObject msgChat = new JSONObject();

        File targetFolder = new File(outPath);
        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }


        // Construct neural network
        Block block = new Mlp(Mnist.IMAGE_HEIGHT * Mnist.IMAGE_WIDTH, Mnist.NUM_CLASSES, new int[]{128, 64});

        try (Model model = Model.newInstance("mlp")) {
            model.setBlock(block);

            // get training and validation dataset
            RandomAccessDataset trainingSet = Mnist.builder().optUsage(Dataset.Usage.TRAIN).setSampling(64, true).optLimit(Long.MAX_VALUE).build();
            trainingSet.prepare(new ProgressBar());

            RandomAccessDataset validateSet = Mnist.builder().optUsage(Dataset.Usage.TEST).setSampling(64, true).optLimit(Long.MAX_VALUE).build();
            validateSet.prepare(new ProgressBar());


            // setup training configuration
            DefaultTrainingConfig config = new DefaultTrainingConfig(Loss.softmaxCrossEntropyLoss())
                    .addEvaluator(new Accuracy())
                    .addTrainingListeners(TrainingListener.Defaults.logging(outPath));

            try (Trainer trainer = model.newTrainer(config)) {
                trainer.setMetrics(new Metrics());

                Shape inputShape = new Shape(1, Mnist.IMAGE_HEIGHT * Mnist.IMAGE_WIDTH);

                // initialize trainer with proper input shape
                trainer.initialize(inputShape);
                //EasyTrain.fit(trainer, 1, trainingSet, validateSet);
                numEpoch = 2;
                for(int epoch = 0; epoch < numEpoch; ++epoch) {
                    Iterator trainIter = trainer.iterateDataset(trainingSet).iterator();

                    while(trainIter.hasNext()) {
                        Batch batch = (Batch)trainIter.next();
                        trainBatch(trainer, batch);
                        trainer.step();
                        batch.close();

                        Long progress = batch.getProgress() * 100 / batch.getProgressTotal();
                        if(pctProgress != progress.intValue() && progress%5==0){
                            pctProgress = progress.intValue();
                            // report per 5%
                            List<Metric> measures = trainer.getMetrics().getMetric("train_progress_Accuracy");
                            MlTrainEpochIndType trainInd = new MlTrainEpochIndType();
                            trainInd.stage = "train";
                            trainInd.epoch = epoch;
                            trainInd.numEpoch = numEpoch;
                            trainInd.progress = pctProgress;
                            trainInd.accuracy = measures.get(measures.size()-1).getValue().floatValue();
                            msgChat.clear();
                            msgChat.set("trainInd", trainInd);
                            stompService.p2pMessage(msgTarget, null, msgChat.toString());
                        }
                    }

                    if (validateSet != null) {
                        Iterator validateIter = trainer.iterateDataset(validateSet).iterator();

                        while(validateIter.hasNext()) {
                            Batch batch = (Batch)validateIter.next();
                            validateBatch(trainer, batch);
                            batch.close();

                            Long progress = batch.getProgress() * 100 / batch.getProgressTotal();
                            if(pctProgress != progress.intValue() && progress%10==0){
                                pctProgress = progress.intValue();
                                // report per 10%
                                MlTrainEpochIndType trainInd = new MlTrainEpochIndType();
                                trainInd.stage = "eval";
                                trainInd.epoch = epoch;
                                trainInd.numEpoch = numEpoch;
                                trainInd.progress = pctProgress;

                                msgChat.clear();
                                msgChat.set("trainInd", trainInd);
                                stompService.p2pMessage(msgTarget, null, msgChat.toString());
                            }
                        }
                    }

                    trainer.notifyListeners((listener) -> { listener.onEpoch(trainer); });

                    Float acc = trainer.getTrainingResult().getEvaluations().get("validate_Accuracy");
                    MlTrainEpochIndType trainInd = new MlTrainEpochIndType();
                    trainInd.stage = "eval";
                    trainInd.epoch = epoch;
                    trainInd.numEpoch = numEpoch;
                    trainInd.accuracy = acc;
                    msgChat.clear();
                    msgChat.set("trainInd", trainInd);
                    stompService.p2pMessage(msgTarget, null, msgChat.toString());
                }



            }
            model.save(Paths.get(outPath), "DJL_model");
            msgChat.clear();
            msgChat.set("status", 0);
            stompService.p2pMessage(msgTarget, null, msgChat.toString());
        } catch (TranslateException e) {
            e.printStackTrace();
        }
        finally {
            logger.info("TARGET<<<"+msgTarget);
        }

        return CompletableFuture.completedFuture(0);
    }

    @Async
    public CompletableFuture<Integer>  executeDJL(String targetModel, String targetFile, String outPath, String msgTarget) throws IOException {
        JSONObject msgChat = new JSONObject();
        File targetFolder = new File(outPath);
        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }

        Path imageFile = Paths.get(targetFile);
        Image img = ImageFactory.getInstance().fromFile(imageFile);
        img.getWrappedImage();

        String backbone;
        String engine = Engine.getDefaultEngineName();
        if ("TensorFlow".equals(Engine.getDefaultEngineName())) {
            backbone = "mobilenet_v2";
        } else {
            backbone = "resnet50";
        }


        Criteria<Image, DetectedObjects> criteria =
                Criteria.builder()
                        .optApplication(Application.CV.OBJECT_DETECTION)
                        .setTypes(Image.class, DetectedObjects.class)
                        .optEngine(Engine.getDefaultEngineName())
                        .optFilter("backbone", backbone)
                        .optProgress(new ProgressBar())
                        .build();

        try (ZooModel<Image, DetectedObjects> model = criteria.loadModel()) {
            try (Predictor<Image, DetectedObjects> predictor = model.newPredictor()) {
                DetectedObjects detection = predictor.predict(img);
                img.drawBoundingBoxes(detection);
                img.save(Files.newOutputStream(imageFile), "png");

                msgChat.clear();
                msgChat.set("prediction", detection.getAsString());
                stompService.p2pMessage(msgTarget, null, msgChat.toString());
            }
        } catch (ModelNotFoundException | MalformedModelException | TranslateException e) {
            msgChat.clear();
            msgChat.set("prediction", "{}");
            stompService.p2pMessage(msgTarget, null, msgChat.toString());
            e.printStackTrace();
        }
        finally {
            //
        }

        return CompletableFuture.completedFuture(0);
    }

    @Async
    public CompletableFuture<Integer>  executeDJL(String modelPath, List<String> modelFiles, String framework, String targetFile, String outPath, String msgTarget) throws IOException {
        JSONObject msgChat = new JSONObject();
        File targetFolder = new File(outPath);
        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }

        // class file
        String synsetFileName = null;
        for(String fs: modelFiles){
            if(fs.endsWith(".txt")){
                synsetFileName = fs;
                break;
            }
        }

        // image file
        Path imageFile = Paths.get(targetFile);
        Image img = ImageFactory.getInstance().fromFile(imageFile);
        img.getWrappedImage();

        switch (framework){
            case "mxnet": {
                try {
                    // model file name
                    String modelFileName = null;
                    for(String fs: modelFiles){
                        if(fs.endsWith(".params")){
                            modelFileName = fs;
                            break;
                        }
                    }

                    Pipeline pipeline = new Pipeline();
                    pipeline.add(new CenterCrop()).add(new Resize(224, 224)).add(new ToTensor());
                    Translator<Image, Classifications> translator = ImageClassificationTranslator.builder()
                            .setPipeline(pipeline)
                            .optSynsetArtifactName(synsetFileName)
                            .optApplySoftmax(true)
                            .build();

                    Model model = Model.newInstance("mxnet");
                    model.load(Paths.get(modelPath));
                    Predictor<Image, Classifications> predictor = model.newPredictor(translator);
                    Classifications classifications = predictor.predict(img);

                    msgChat.clear();
                    msgChat.set("prediction", classifications.getAsString());
                    stompService.p2pMessage(msgTarget, null, msgChat.toString());
                } catch ( Exception e) {
                    msgChat.clear();
                    msgChat.set("prediction", "{}");
                    stompService.p2pMessage(msgTarget, null, msgChat.toString());
                    e.printStackTrace();
                }
                finally {
                    break;
                }
            }
            case "pytorch": {

                Translator<Image, Classifications> mnistTranslator = new Translator<Image, Classifications>() {
                    @Override
                    public NDList processInput(TranslatorContext ctx, Image input) {
                        // Convert Image to NDArray
                        NDArray array = input.toNDArray(ctx.getNDManager(), Image.Flag.GRAYSCALE);
                        return new NDList(NDImageUtils.toTensor(array));
                    }

                    @Override
                    public Classifications processOutput(TranslatorContext ctx, NDList list) {
                        // Create a Classifications with the output probabilities
                        NDArray probabilities = list.singletonOrThrow().softmax(0);
                        List<String> classNames = IntStream.range(0, 10).mapToObj(String::valueOf).collect(Collectors.toList());
                        return new Classifications(classNames, probabilities);
                    }

                    @Override
                    public Batchifier getBatchifier() {
                        // The Batchifier describes how to combine a batch together
                        // Stacking, the most common batchifier, takes N [X1, X2, ...] arrays to a single [N, X1, X2, ...] array
                        return Batchifier.STACK;
                    }
                };



                Pipeline pipeline = new Pipeline();
                pipeline.add(new Resize(256))
                        .add(new CenterCrop(224, 224))
                        .add(new ToTensor())
                        .add(new Normalize(
                                new float[]{0.485f, 0.456f, 0.406f},
                                new float[]{0.229f, 0.224f, 0.225f}));

                Translator<Image, Classifications> translator = ImageClassificationTranslator.builder()
                        .setPipeline(pipeline)
                        .optSynsetArtifactName(synsetFileName)
                        .optApplySoftmax(true)
                        .build();


                Criteria<Image, Classifications> criteria = Criteria.builder()
                        .setTypes(Image.class, Classifications.class)
                        .optModelPath(Paths.get(modelPath))
                        //.optModelName("resnet18.pt")
                        .optOption("mapLocation", "true") // this model requires mapLocation for GPU
                        .optEngine("PyTorch")
                        .optTranslator(modelPath.contains("mnist")?mnistTranslator:translator)
                        .optProgress(new ProgressBar()).build();

                try (ZooModel model = criteria.loadModel()){
                    Predictor<Image, Classifications> predictor = model.newPredictor();
                    Classifications classifications = predictor.predict(img);

                    msgChat.clear();
                    msgChat.set("prediction", classifications.getAsString());
                    stompService.p2pMessage(msgTarget, null, msgChat.toString());
                }catch ( Exception e) {
                    msgChat.clear();
                    msgChat.set("prediction", "{}");
                    stompService.p2pMessage(msgTarget, null, msgChat.toString());
                    e.printStackTrace();
                }
                finally {
                    System.out.print("done!");
                }

                break;

            }
            case "tensorflow": {
                Criteria<Image, Classifications> criteria =
                        Criteria.builder()
                                .setTypes(Image.class, Classifications.class)
                                .optEngine("TensorFlow")
                                .optTranslator(new TfTranslator())
                                .optProgress(new ProgressBar())
                                .build();
                try (ZooModel model = criteria.loadModel()){
                    Predictor<Image, Classifications> predictor = model.newPredictor();
                    Classifications classifications = predictor.predict(img);

                    msgChat.clear();
                    msgChat.set("prediction", classifications.getAsString());
                    stompService.p2pMessage(msgTarget, null, msgChat.toString());
                }catch ( Exception e) {
                    msgChat.clear();
                    msgChat.set("prediction", "{}");
                    stompService.p2pMessage(msgTarget, null, msgChat.toString());
                    e.printStackTrace();
                }
                finally {
                    System.out.print("done!");
                }

                break;
            }
        }



        return CompletableFuture.completedFuture(0);
    }

}
