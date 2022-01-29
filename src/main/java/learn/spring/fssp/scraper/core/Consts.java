package learn.spring.fssp.scraper.core;

import learn.spring.fssp.scraper.core.dao.ProductionRepository;

public class Consts {

    public static final String CONTROLLER_PATH = "/fssp";
    public static final String TASK_TABLE = "FSSP_TASK";
    public static final String PRODUCTION_TABLE = "PRODUCTIONS";
    public static final String PROXIES_TABLE = "PROXIES";
    public static final String SCHEDULER_DELAY = "2000";

    public static final String BIRTHDATE_PATTERN = "dd.MM.yyyy";
    public static final String DATETIME_PATTERN = "dd.MM.yyyy'T'HH:mm:ss.SSSZ";
}
