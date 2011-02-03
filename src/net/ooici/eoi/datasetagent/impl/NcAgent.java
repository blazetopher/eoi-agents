/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.ooici.eoi.datasetagent.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import net.ooici.eoi.datasetagent.AbstractNcAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.NetcdfDataset;

/**
 *
 * @author cmueller
 */
public class NcAgent extends AbstractNcAgent {

    private static final Logger log = LoggerFactory.getLogger(NcAgent.class);

    private String sTime = null, eTime = null;

    public String buildRequest(net.ooici.services.sa.DataSource.EoiDataContext context) {
        String ncmlTemplate = context.getNcmlMask();
        String ncdsLoc = context.getDatasetUrl();
        sTime = context.getStartTime();
        eTime = context.getEndTime();
        
        String ncmlPath = buildNcmlMask(ncmlTemplate, ncdsLoc);
        log.debug(ncmlPath);
        return ncmlPath;
    }

    public Object acquireData(String request) {
        NetcdfDataset ncds = null;
        try {
            ncds = NetcdfDataset.openDataset(request);
        } catch (IOException ex) {
            log.error("Error opening dataset \"" + request + "\"", ex);
        }
        log.debug("\n" + ncds);
        return ncds;
    }

    public String[] processDataset(NetcdfDataset ncds) {
        String response = this.sendNetcdfDataset(ncds, "ingest", false);
        return new String[]{response};
    }

    private String buildNcmlMask(String content, String ncdsLocation) {
        BufferedWriter writer = null;
        String temploc = null;
        try {
            content = content.replace("***lochold***", ncdsLocation);
            File tempFile = File.createTempFile("ooi", ".ncml");
            tempFile.deleteOnExit();
            temploc = tempFile.getCanonicalPath();
            writer = new BufferedWriter(new FileWriter(tempFile));
            writer.write(content);
        } catch (IOException ex) {
            log.error("Error generating ncml mask for dataset at \"" + ncdsLocation + "\"");
        } finally {
            try {
                writer.close();
            } catch (IOException ex) {
                log.error("Error closing ncml template writer");
            }
        }

        return temploc;
    }

    public static void main(String[] args) {
        try {
            ion.core.IonBootstrap.bootstrap();
        } catch (Exception ex) {
            log.error("Error bootstrapping", ex);
        }
        /* the ncml mask to use*/
        /* for NAM - WARNING!!  This is a HUGE file... not really supported properly yet... */
        String ncmlmask = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> <netcdf xmlns=\"http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\" location=\"***lochold***\"> <variable name=\"time\"> <attribute name=\"standard_name\" value=\"time\" /> </variable> <variable name=\"lat\"> <attribute name=\"standard_name\" value=\"latitude\" /> <attribute name=\"units\" value=\"degree_north\" /> </variable> <variable name=\"lon\"> <attribute name=\"standard_name\" value=\"longitude\" /> <attribute name=\"units\" value=\"degree_east\" /> </variable> </netcdf>";
        String dataurl = "http://nomads.ncep.noaa.gov:9090/dods/nam/nam20110131/nam1hr_00z";

        /* for HiOOS Gliders */
        ncmlmask = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><netcdf xmlns=\"http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\" location=\"***lochold***\"><variable name=\"pressure\"><attribute name=\"coordinates\" value=\"time longitude latitude depth\"/></variable><variable name=\"temp\"><attribute name=\"coordinates\" value=\"time longitude latitude depth\"/></variable><variable name=\"conductivity\"><attribute name=\"coordinates\" value=\"time longitude latitude depth\"/></variable><variable name=\"salinity\"><attribute name=\"coordinates\" value=\"time longitude latitude depth\"/></variable><variable name=\"density\"><attribute name=\"coordinates\" value=\"time longitude latitude depth\"/></variable></netcdf>";
        dataurl = "http://oos.soest.hawaii.edu/thredds/dodsC/hioos/glider/sg139_8/p1390877.nc";

        /* Generic testing */
        ncmlmask = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><netcdf xmlns=\"http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\" location=\"***lochold***\"></netcdf>";
        dataurl = "http://thredds1.pfeg.noaa.gov/thredds/dodsC/satellite/GR/ssta/1day";

        net.ooici.services.sa.DataSource.EoiDataContext.Builder cBldr = net.ooici.services.sa.DataSource.EoiDataContext.newBuilder();
        cBldr.setDatasetUrl(dataurl).setNcmlMask(ncmlmask);
        cBldr.setStartTime("2011-02-01T00:00:00Z");
        cBldr.setStartTime("2011-02-02T00:00:00Z");

        net.ooici.services.sa.DataSource.EoiDataContext context = cBldr.build();
//
//        Map<String, String[]> context = new HashMap<String, String[]>();
//        context.put(DataSourceRequestKeys.BASE_URL, new String[] {dataurl});
//        context.put("ncml_mask", new String[] {ncmlmask});
        net.ooici.eoi.datasetagent.IDatasetAgent agent = new NcAgent();
        agent.setTesting(true);

        java.util.HashMap<String, String> connInfo = new java.util.HashMap<String, String>();
        connInfo.put("exchange", "eoitest");
        connInfo.put("service", "eoi_ingest");
        connInfo.put("server", "macpro");
        connInfo.put("topic", "magnet.topic");
        String[] result = agent.doUpdate(context, connInfo);
        log.debug("Response:");
        for (String s : result) {
            log.debug(s);
        }

//        String outdir = "output/nc/";
//        try {
//            if (!new File(outdir).exists()) {
//                new File(outdir).mkdirs();
//            }
//
//            ucar.nc2.FileWriter.writeToFile(dataset, outdir + dataurl.substring(dataurl.lastIndexOf("/") + 1), false, 0, true);
//        } catch (IOException ex) {
//            log.warn("Could not write NC to file", ex);
//        }
    }
}
