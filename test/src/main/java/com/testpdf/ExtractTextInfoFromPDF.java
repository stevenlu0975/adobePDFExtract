import com.adobe.pdfservices.operation.PDFServices;
import com.adobe.pdfservices.operation.PDFServicesMediaType;
import com.adobe.pdfservices.operation.PDFServicesResponse;
import com.adobe.pdfservices.operation.auth.Credentials;
import com.adobe.pdfservices.operation.auth.ServicePrincipalCredentials;
import com.adobe.pdfservices.operation.exception.SDKException;
import com.adobe.pdfservices.operation.exception.ServiceApiException;
import com.adobe.pdfservices.operation.exception.ServiceUsageException;
import com.adobe.pdfservices.operation.io.Asset;
import com.adobe.pdfservices.operation.io.StreamAsset;
import com.adobe.pdfservices.operation.pdfjobs.jobs.ExtractPDFJob;
import com.adobe.pdfservices.operation.pdfjobs.params.extractpdf.ExtractElementType;
import com.adobe.pdfservices.operation.pdfjobs.params.extractpdf.ExtractPDFParams;
import com.adobe.pdfservices.operation.pdfjobs.result.ExtractPDFResult;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ExtractTextInfoFromPDF {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtractTextInfoFromPDF.class);

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
//        try (InputStream inputStream = Files.newInputStream(new File("src/main/resources/Adobe Extract API Sample.pdf").toPath())) {
//        try (InputStream inputStream = Files.newInputStream(new File("src/main/resources/77777.pdf").toPath())) {
//        try (InputStream inputStream = Files.newInputStream(new File("src/main/resources/resume.pdf").toPath())) {
        try (InputStream inputStream = Files.newInputStream(new File("src/main/resources/vertical.pdf").toPath())) {
            // Initial setup, create credentials instance
            Credentials credentials = new ServicePrincipalCredentials(
                    System.getenv("PDF_SERVICES_CLIENT_ID"),
                    System.getenv("PDF_SERVICES_CLIENT_SECRET")
            );
          
            // Creates a PDF Services instance
            PDFServices pdfServices = new PDFServices(credentials);
          
            // Creates an asset(s) from source file(s) and upload
            Asset asset = pdfServices.upload(inputStream, PDFServicesMediaType.PDF.getMediaType());
          
            // Create parameters for the job
            ExtractPDFParams extractPDFParams = ExtractPDFParams.extractPDFParamsBuilder()
                    .addElementsToExtract(Arrays.asList(ExtractElementType.TEXT)).build();
          
            // Creates a new job instance
            ExtractPDFJob extractPDFJob = new ExtractPDFJob(asset)
                    .setParams(extractPDFParams);
          
            // Submit the job and gets the job result
            String location = pdfServices.submit(extractPDFJob);
            PDFServicesResponse<ExtractPDFResult> pdfServicesResponse = pdfServices.getJobResult(location, ExtractPDFResult.class);
          
            // Get content from the resulting asset(s)
            Asset resultAsset = pdfServicesResponse.getResult().getResource();
            StreamAsset streamAsset = pdfServices.getContent(resultAsset);
          
            // Creates an output stream and copy stream asset's content to it
            Files.createDirectories(Paths.get("output/"));
//            String zipFileOutputPath = "output/ExtractTextInfoFromPDF.zip";
            String zipFileOutputPath = "output/77776.zip";
            OutputStream outputStream = Files.newOutputStream(new File(zipFileOutputPath).toPath());
            LOGGER.info("Saving asset at output/ExtractTextInfoFromPDF.pdf");
            IOUtils.copy(streamAsset.getInputStream(), outputStream);
            outputStream.close();

            ZipFile resultZip = new ZipFile(zipFileOutputPath);
            ZipEntry jsonEntry = resultZip.getEntry("structuredData.json");
            InputStream is = resultZip.getInputStream(jsonEntry);
            Scanner s = new Scanner(is).useDelimiter("\\A");
            String jsonString = s.hasNext() ? s.next() : "";
            s.close();
            
            JSONObject jsonData = new JSONObject(jsonString);
            JSONArray elements = jsonData.getJSONArray("elements");
            for(int i=0; i < elements.length(); i++) {
                JSONObject element = elements.getJSONObject(i);
                String path = element.getString("Path");
                if(path.endsWith("/H1")) {
                    String text = element.getString("Text");
                    System.out.println(text);
                }
            }
        } catch (ServiceApiException | IOException | SDKException | ServiceUsageException e) {
            LOGGER.error("Exception encountered while executing operation", e);
        }
        System.out.println(System.currentTimeMillis()-startTime);
    }
}
