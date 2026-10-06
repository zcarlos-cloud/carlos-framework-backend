package com.carlos.fx.codege.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.ds.simple.SimpleDataSource;
import com.carlos.fx.codege.enums.DbTypeEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 数据库连接相关工具
 *
 * @author Carlos
 * @date 2019/4/26 11:17
 * @ModificationHistory Who  When  What ---------     -------------   --------------------------------------
 */
@Accessors(chain = true)
@Data
public class DatabaseInfo {

    /**
     * 数据库类型
     */
    private DbTypeEnum dbType;
    /**
     * 数据库IP地址
     */
    private String ip = "127.0.0.1";
    /**
     * 端口号
     */
    private String port = "3306";
    /**
     * 数据库名
     */
    private String dbName;
    /**
     * url
     */
    private String url;
    /**
     * 数据库用户名
     */
    private String user = "root";
    /**
     * 数据库密码
     */
    private String pwd;


    /**
     * 获取数据库连接地址
     *
     * @return 数据库连接地址
     * @author Carlos
     * @date 2019/12/25 17:53
     */
    public String buildUrl() {
        String url = this.getDbType().getUrl();
        if (StrUtil.isNotEmpty(this.getDbName())) {
            url = url.replace("[databaseName]", this.getDbName());
        } else {
            url = url.replace("[databaseName]", "");
        }
        url = url.replace("[ip]", this.getIp());
        url = url.replace("[port]", this.getPort());
        return url;
    }

    /**
     * 根据表单信息创建数据源对象
     *
     * @return cn.hutool.db.ds.simple.SimpleDataSource
     * @author Carlos
     * @date 2021/11/11 23:14
     */
    public SimpleDataSource getDataSource() {
        SimpleDataSource dataSource = new SimpleDataSource(buildUrl(), this.user, this.pwd, this.getDbType().getDriver());
        Properties props = new Properties();
        // 设置可以获取remarks信息
        props.setProperty("remarks", "true");
        // 设置可以获取tables remarks信息
        props.setProperty("useInformationSchema", "true");
        props.put("remarksReporting", "true");
        dataSource.setConnProps(props);
        return dataSource;
    }

    public Connection getConnection() throws SQLException {
        Db db = Db.use(getDataSource());
        return db.getConnection();
    }
}
