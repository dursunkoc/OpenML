package org.openml.webapplication.features;

import org.json.JSONArray;
import org.json.JSONException;
import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.algorithms.QueryUtils;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.xml.DataSetDescription;
import weka.core.Instances;

import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


public class DatabaseUtils {
    public DatabaseUtils(OpenmlConnector connector){
        this.apiconnector = connector;
    }
    private OpenmlConnector apiconnector;

    public List<String> GetQualitiesAvailable(int datasetId, Integer window_size) throws Exception {
        List<String> qualitiesAvailable = Arrays.asList(apiconnector.dataQualities(datasetId).getQualityNames());
        if (window_size != null) {
            // alternative approach to knowing which features are already complete.
            String sql =
                    "SELECT i.quality, CEIL(`q`.`value` / " + window_size + ") AS `numIntervals`, COUNT(*) AS `present` " +
                            "FROM `data_quality` `q`, `dataset` `d` LEFT JOIN `data_quality_interval` `i` ON `d`.`did` = `i`.`data` AND `i`.`interval_end` - `i`.`interval_start` =  " + window_size + " " +
                            "WHERE `d`.`did` = `q`.`data` AND `q`.`quality` = 'NumberOfInstances'  AND `d`.`error` = 'false' AND `d`.`processed` IS NOT NULL AND d.did = " + datasetId + " " +
                            "GROUP BY `d`.`did`,`i`.`quality` HAVING `present` = `numIntervals`";
            Conversion.log("OK", "FantailQuery for interval queries", sql);
            qualitiesAvailable = Arrays.asList(QueryUtils.getStringsFromDatabase(apiconnector, sql));
        }
        return qualitiesAvailable;
    }

    public DataSetDescription GetDatasetDescription(int did) throws Exception {
        return apiconnector.dataGet(did);
    }

    public Instances getDataset(DataSetDescription dsd) throws Exception
    {
        Conversion.log("OK", "Extract Features", "Start downloading dataset: " + dsd.getId());

        Instances dataset = new Instances(new FileReader(dsd.getDataset(apiconnector.getApiKey())));

        if (dsd.getDefault_target_attribute() == null) {
            throw new RuntimeException("Default target attribute is null. ");
        }

        return dataset;
    }

    public Integer getDatasetId(int expectedQualities, Integer window_size, boolean random, String priorityTag) throws JSONException, Exception {
        String tagJoin = "";
        String tagSelect = "";
        String tagSort = "";
        if (priorityTag != null) {
            tagSelect = ", t.tag ";
            tagSort = "t.tag DESC, "; // to avoid NULL values first
            tagJoin = "LEFT JOIN dataset_tag t ON q.data = t.id AND t.tag = '" + priorityTag + "' ";
        }

        String sql =
                "SELECT `d`.`did`, `q`.`value` AS `numInstances`, `i`.`interval_end` - `i`.`interval_start` AS `interval_size`, " +
                        "CEIL(`q`.`value` / " + window_size + ") AS `numIntervals`, " +
                        "(COUNT(*) / CEIL(`q`.`value` / " + window_size + ")) AS `qualitiesPerInterval`, " +
                        "COUNT(*) AS `qualities` " + tagSelect +
                        "FROM `data_quality` `q` " + tagJoin +
                        ", `dataset` `d`" +
                        "LEFT JOIN `data_quality_interval` `i` ON `d`.`did` = `i`.`data` AND `i`.`interval_end` - `i`.`interval_start` =  " + window_size + " " +
                        "WHERE `q`.`quality` IS NOT NULL " +
                        "AND `d`.`did` = `q`.`data` " +
                        "AND `q`.`quality` = 'NumberOfInstances'  " +
                        "AND `d`.`error` = 'false' AND `d`.`processed` IS NOT NULL " +
                        "GROUP BY `d`.`did` " +
                        "HAVING (COUNT(*) / CEIL(`q`.`value` / " + window_size + ")) < " + expectedQualities + " " +
                        "ORDER BY " + tagSort + "`qualitiesPerInterval` ASC LIMIT 0,100; ";

        if(window_size == null) {
            sql =
                    "SELECT q.data, COUNT(*) AS `numQualities`" + tagSelect +
                            " FROM data_quality q " + tagJoin +
                            " GROUP BY q.data HAVING numQualities BETWEEN 0 AND " + (expectedQualities-1) +
                            " ORDER BY " + tagSort + " q.data LIMIT 0,100";
        }

        Conversion.log("OK", "FantailQuery", sql);
        JSONArray runJson = (JSONArray) apiconnector.freeQuery(sql).get("data");


        int randomint = 0;

        if (random) {
            Random randomgen = new Random(System.currentTimeMillis());
            randomint = Math.abs(randomgen.nextInt());
        }

        if(runJson.length() > 0) {
            int dataset_id = ((JSONArray) runJson.get(randomint % runJson.length())).getInt(0);
            return dataset_id;
        } else {
            return null;
        }
    }
}
