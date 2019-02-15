package remoteagent.beans;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFPrintSetup;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import static remoteagent.beans.AgentBase.dbConfig;
import static remoteagent.beans.AgentBase.dbData;

/**
 *
 * @author yura_
 */
public class ExcelReport extends AgentBase{
    private final List<String> archieveTags = new ArrayList<>();
    private final List<String> archieveTagsDesc = new ArrayList<>();
    private boolean shiftStarted;
    private int shift, currentShiftID;
    private boolean reportDone = false;
    private int oldShift;
    private Date selectedDate;
    private boolean build = false;

    public boolean isBuild() {
        return build;
    }

    public void setBuild(boolean build) {
        this.build = build;
    }
    
    public int getShift() {
        return shift;
    }

    public void setShift(int shift) {
        this.shift = shift;
    }

    public Date getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(Date selectedDate) {
        this.selectedDate = selectedDate;
    }
    public ExcelReport(){
        super();
    }          
    
    //Получение задания агентом
    @Override
    public void getTask(){              
        reportDone=false;
        try {
            Statement stmt = dbConfig.db.createStatement();
            ResultSet rs;
            rs = stmt.executeQuery("select PROP_VALUE from viewLevelTags where VAR_CLASS=0 and name=4 order by NODE");
            while (rs.next()){
                archieveTags.add(rs.getString(1));                
            }
            rs.close();
            rs = stmt.executeQuery("select PROP_VALUE from viewLevelTags where VAR_CLASS=7 and name=4 order by NODE");
            while (rs.next()){
                archieveTagsDesc.add(rs.getString(1));                
            }
            rs.close();
            stmt.close();                                    
        } catch (SQLException ex) {
            Logger.getLogger(ExcelReport.class.getName()).log(Level.SEVERE, null, ex);
            getDbConnection();
            getTask();
        }
    }
    @Override
    public void doTask(){
        
        if (isBuild()){
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");                                 
                        
            //Если началась новая смена - создаем файл отчета для предыдущей смены                            
                Date reportDate = getSelectedDate();                
                                
                HSSFWorkbook wb = new HSSFWorkbook();
                HSSFCellStyle headerCellStyle = wb.createCellStyle();
                HSSFFont headerFont = wb.createFont();
                headerFont.setBold(true);
                headerCellStyle.setAlignment(HorizontalAlignment.CENTER_SELECTION);
                headerCellStyle.setBorderBottom(CellStyle.BORDER_MEDIUM);
                headerCellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
                headerCellStyle.setBorderLeft(CellStyle.BORDER_MEDIUM);
                headerCellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
                headerCellStyle.setBorderRight(CellStyle.BORDER_MEDIUM);
                headerCellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
                headerCellStyle.setBorderTop(CellStyle.BORDER_MEDIUM);
                headerCellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
                headerCellStyle.setFont(headerFont);
                headerCellStyle.setAlignment(HorizontalAlignment.CENTER_SELECTION);
                
                HSSFCellStyle dataCellStyle = wb.createCellStyle();
                //dataCellStyle.setAlignment(HorizontalAlignment.CENTER_SELECTION);
                dataCellStyle.setBorderBottom(CellStyle.BORDER_THIN);
                dataCellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
                dataCellStyle.setBorderLeft(CellStyle.BORDER_THIN);
                dataCellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
                dataCellStyle.setBorderRight(CellStyle.BORDER_THIN);
                dataCellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
                dataCellStyle.setBorderTop(CellStyle.BORDER_THIN);
                dataCellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
                String fileName = dateFormat.format(reportDate);
                if (getShift()==1){
                    fileName = fileName+"_08-00";
                }else{
                    fileName = fileName+"_20-00";
                }
                try (FileOutputStream fileOut = new FileOutputStream("reports\\"+fileName+".xls")) {
                    HSSFSheet sheet = wb.createSheet(fileName);                                        
                    HSSFRow title = sheet.createRow(0);
                    HSSFCell titleCell = title.createCell(0);
                    titleCell.setCellValue(fileName);
                    
                    HSSFCell operator = title.createCell(1);
                    operator.setCellValue("Старший оператор:");
                    HSSFCell operatorName = title.createCell(2);
                    Statement opStm = dbData.db.createStatement();
                    ResultSet opRS = opStm.executeQuery("SELECT TOP 1 aDesc, id FROM dbo.ProcessArchieve where aDate = '"+
                                       dateFormat.format(reportDate)+
                                       "' and aShift="+String.valueOf(getShift())+" order by id desc");
                    while (opRS.next()){
                        operatorName.setCellValue(opRS.getString(1));
                    }
                    opRS.close();
                    opStm.close();
                    int newRow=2;
                    
                    //Заполнение заголовка
                    HSSFRow headerRow = sheet.createRow(newRow);
                    HSSFCell nameCell = headerRow.createCell(0);
                    nameCell.setCellStyle(headerCellStyle);
                    nameCell.setCellValue("Параметр");
                    nameCell.setCellStyle(headerCellStyle);
                    
                    HSSFCell minCell = headerRow.createCell(1);
                    minCell.setCellValue("Мин. значение");
                    minCell.setCellStyle(headerCellStyle);
                    
                    HSSFCell maxCell = headerRow.createCell(2);
                    maxCell.setCellValue("Макс. значение");
                    maxCell.setCellStyle(headerCellStyle);
                    
                    HSSFCell avgCell = headerRow.createCell(3);
                    avgCell.setCellValue("Средн. значение");
                    avgCell.setCellStyle(headerCellStyle);
                    
                    //Заполнение заголовка параметров и данных
                    for (int i=0; i<archieveTagsDesc.size(); i++){                                                                        
                        newRow++;                        
                                          
                        String maxValue = null;
                        String minValue = null;
                        String avgValue = null;
                        try (Statement stmt = dbData.db.createStatement()) {
                            String query = "SELECT aTag, MaxValue, MinValue, AvgValue FROM dbo.ExcelReport_Data where aDate = '"+
                                    dateFormat.format(reportDate)+
                                    "' and aShift="+String.valueOf(getShift())+
                                    " and aTag='"+archieveTags.get(i)+"'";
                            ResultSet rs;
                            //System.out.println(query);
                            rs = stmt.executeQuery(query);
                            while (rs.next()){
                                maxValue = rs.getString(2);
                                minValue = rs.getString(3);
                                avgValue = rs.getString(4);
                            }
                            HSSFRow detailedRow = sheet.createRow(newRow);
                            HSSFCell paramCell = detailedRow.createCell(0);
                            paramCell.setCellValue(archieveTagsDesc.get(i));
                            paramCell.setCellStyle(dataCellStyle);
                            HSSFCell valueCellMin = detailedRow.createCell(1);
                            valueCellMin.setCellValue(minValue);
                            valueCellMin.setCellStyle(dataCellStyle);                            
                            HSSFCell valueCellMax = detailedRow.createCell(2);
                            valueCellMax.setCellValue(maxValue);
                            valueCellMax.setCellStyle(dataCellStyle);
                            HSSFCell valueCellAvg = detailedRow.createCell(3);
                            valueCellAvg.setCellValue(String.format("%.6s", avgValue));
                            valueCellAvg.setCellStyle(dataCellStyle);
                            rs.close();
                        }                                                
                    } 
                    sheet.autoSizeColumn(0);
                    sheet.autoSizeColumn(1);
                    sheet.autoSizeColumn(2);
                    sheet.autoSizeColumn(3);
                    HSSFPrintSetup printSetup = sheet.getPrintSetup();
                    printSetup.setScale((short)85);
                    
                    wb.write(fileOut);
                    wb.close();
                    System.out.println("Making report file "+fileName+".xls complete");
                    Logger.getLogger(ExcelReport.class.getName()).log(Level.INFO, "Making report file "+fileName+".xls complete");
                    reportDone = true;
                    JOptionPane.showMessageDialog(null, "Making report file "+fileName+".xls complete");
                    setBuild(false);
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(ExcelReport.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException | SQLException ex) {
                    Logger.getLogger(ExcelReport.class.getName()).log(Level.SEVERE, null, ex);
                    getDbConnection();
                    getTask();
                }
            }            
        
    }
    
    private void buildReport(){
        
    }
    
    @Override
    @SuppressWarnings("empty-statement")
    public void run() {
        while (true){
            synchronized(this){                
                doTask();   
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ExcelReport.class.getName()).log(Level.SEVERE, null, ex);
                }
            }    
        }    
    }
}
