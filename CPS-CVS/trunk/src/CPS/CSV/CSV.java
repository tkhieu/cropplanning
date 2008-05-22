/* CSV.java - Created on April 3, 2007
 * Copyright (C) 2007, 2008 Clayton Carter
 *
 * This file is part of the project "Crop Planning Software".  For more
 * information:
 *    website: http://cropplanning.googlecode.com
 *    email:   cropplanning@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package CPS.CSV;

import CPS.Data.*;
import CPS.Module.CPSExporter;
import CPS.Module.CPSImporter;
import CPS.Module.CPSDataModelConstants;
import CPS.Module.CPSModule;
import java.util.ArrayList;
import javax.swing.table.TableModel;
import com.csvreader.*;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;


public class CSV extends CPSModule implements CPSExporter, CPSImporter {


  CSVTableModel ctm;
  boolean exportOnly;
  CPSDateValidator dateValidator;
  private CSVColumnMap columnMap;

  public CSV() {
      setModuleName("CSV");
      setModuleType( MOD_TYPE_DATAMODEL );
      setModuleDescription("A CSV DataModel (for import/export only)");
      setModuleVersion( GLOBAL_DEVEL_VERSION );

      exportOnly = true;
      dateValidator = new CPSDateValidator();

      columnMap = new CSVColumnMap();
  }

  public CSV( String file ) {
      this();
      ctm = new CSVTableModel( file );
      if ( ctm != null )
          exportOnly = false;
  }


  /* left over from when this module was a CPSDataModel */
  public ArrayList<CPSCrop> getCropsAndVarietiesAsList() {
      if ( exportOnly )
          return new ArrayList<CPSCrop>();

     ArrayList<CPSCrop> l = new ArrayList<CPSCrop>();
     CPSCrop temp;

     System.out.println( "Exporting data." );

     for ( int i = 0; i < ctm.getRowCount(); i++ ) {
        l.add( this.getCropInfoForRow(i) );
     }

     return l;
  }

   /* left over from when this module was a CPSDataModel */
  public CPSCrop getCropInfoForRow( int selectedRow ) {

     CPSCrop temp = new CPSCrop();

     if ( exportOnly )
         return temp;

     //"crop_name","var_name","similar_to","bot_name","fam_name",
     //"Groups","Successions","Desc","Keywords","Fudge","other_req","Notes",
     //"Maturity","mat_adjust","misc_adjust",
     //"seeds_sources","seeds_item_codes","seeds_unit_size"

     //temp.setID( (int) ( Math.random() * 10000 ) );
     temp.setID( selectedRow );
     int col = 0;
     temp.setCropName( ctm.getStringAt( selectedRow, col++ ) );
     temp.setVarietyName( ctm.getStringAt( selectedRow, col++ ));

     temp.setSimilarCrop( ctm.getStringAt( selectedRow, col++ ) );

     temp.setBotanicalName( ctm.getStringAt( selectedRow, col++ ));
     temp.setFamilyName( ctm.getStringAt( selectedRow, col++ ));

     temp.setGroups( ctm.getStringAt( selectedRow, col++ ));
     // temp.setSuccessions( Boolean.parseBoolean( ctm.getStringAt( selectedRow, col++ )));
     col++; // skip the successions column
     temp.setCropDescription( ctm.getStringAt( selectedRow, col++ ));
     temp.setKeywords( ctm.getStringAt( selectedRow, col++ ));
     col++; // fudge
     temp.setOtherRequirements( ctm.getStringAt( selectedRow, col++ ));
     temp.setNotes( ctm.getStringAt( selectedRow, col++ ));

     int i = ctm.getIntAt( selectedRow, col++ );
     if ( i <= 0 )
        i = -1;
     temp.setMaturityDays( i );

     return temp;

  }

  /* left over from when this module was a CPSDataModel */
  public CPSCrop getCropInfo( String cropName ) {

     CPSCrop temp = new CPSCrop();

     if ( exportOnly )
         return temp;

     if ( ! cropName.equals("") )
        for ( int i = 0; i < ctm.getRowCount(); i ++ ) {
           // match crop_name == cropName and var_name == ""
           if ( ctm.getStringAt( i, 0 ).equalsIgnoreCase( cropName ) &&
                ctm.getStringAt( i, 1 ).equalsIgnoreCase( "" ) ) {
              temp = getCropInfoForRow( i );
              break;
           }
        }

     return temp;
  }

  
   public void exportCropPlan( String filename, String planName, ArrayList<CPSPlanting> plantings ) {

       ArrayList<CPSRecord> records = new ArrayList<CPSRecord>( plantings.size() );
       for ( CPSPlanting p : plantings )
           records.add( (CPSRecord) p );

       this.exportRecords( filename, "crop plan: " + planName, records );


   }

   public void exportCropsAndVarieties( String filename, ArrayList<CPSCrop> crops ) {

       ArrayList<CPSRecord> records = new ArrayList<CPSRecord>( crops.size() );
       for ( CPSCrop c : crops )
           records.add( (CPSRecord) c );

       this.exportRecords( filename, "Crops and Varieties", records );

   }

   private void exportRecords( String filename, String recordType, ArrayList<CPSRecord> records ) {

       final boolean EXPORT_SPARSE_DATA = true;

       CsvWriter csvOut = new CsvWriter( filename );
       // mark text with double quotes
       csvOut.setTextQualifier('"');
       // set default comment character to hash
       csvOut.setComment('#');

       try {
           // write comment about date, time, etc
           csvOut.writeComment( " Created by CropPlanning Software" );
           csvOut.writeComment( " Available at http://cropplanning.googlecode.com" );
           csvOut.writeComment( " Records exported: " + recordType );
           csvOut.writeComment( " Exported: " + new Date().toString() );

           // collect header information
           CPSRecord c = records.get( 0 );
           CPSDatum d;
           Iterator it = c.iterator();
           HashMap<String, Integer> colMap = new HashMap<String, Integer>();
           int columnCount = 0;
           while ( it.hasNext() ) {
               d = (CPSDatum) it.next();
               colMap.put( d.getColumnName(), new Integer( columnCount++ ) );
           }


           String[] row = new String[columnCount];

           // prepare and write header
           it = c.iterator();
           while ( it.hasNext() ) {
               d = (CPSDatum) it.next();
               int colForDatum = colMap.get( d.getColumnName() ).intValue();
               row[colForDatum] = d.getColumnName();
           }
           csvOut.writeRecord( row );

           // write rows
           for ( CPSRecord record : records ) {
               row = new String[columnCount];
               it = record.iterator();
               while ( it.hasNext() ) {
                   d = (CPSDatum) it.next();
                   int colForDatum = colMap.get( d.getColumnName() ).intValue();

                   if ( EXPORT_SPARSE_DATA && ! d.isConcrete() ) {
                       row[colForDatum] = "";
                       continue;
                   }

                   Object o = d.getDatum();
                   if ( o instanceof java.util.Date ||
                        o instanceof java.sql.Date )
                       row[colForDatum] = dateValidator.format( (Date) d.getDatum() );
                   else
                       row[colForDatum] = d.getDatum().toString();
               }
               csvOut.writeRecord( row );
           }

           // write comment "EOF"
           csvOut.writeComment( " End of file" );

           // close
           csvOut.close();
       }
       catch ( Exception ignore ) { ignore.printStackTrace(); }

   }

   public String getExportFileDefaultExtension() {
       return "csv";
   }


   @Override
   public int init() {
       throw new UnsupportedOperationException( "Not supported yet." );
   }

   @Override
   protected int saveState() {
       throw new UnsupportedOperationException( "Not supported yet." );
   }

   @Override
   public int shutdown() {
       throw new UnsupportedOperationException( "Not supported yet." );
   }

  public String propNameFromPropNum( int recordType, int propertyNum ) {
     if      ( recordType == CPSDataModelConstants.RECORD_TYPE_CROP )
        return columnMap.getCropColumnNameForProperty( propertyNum );
     else if ( recordType == CPSDataModelConstants.RECORD_TYPE_PLANTING )
        return columnMap.getPlantingColumnNameForProperty( propertyNum );
     else
        return "UnknownProperty";
  }

  public int propNumFromPropName( int recordType, String propertyName ) {

     if      ( recordType == CPSDataModelConstants.RECORD_TYPE_CROP )
        return columnMap.getCropPropertyNumFromName( propertyName );
     else if ( recordType == CPSDataModelConstants.RECORD_TYPE_PLANTING )
     {
        return columnMap.getPlantingPropertyNumFromName( propertyName );
     }
     else
        return 0;
  }

   public String getImportFileDefaultExtension() {
       return "csv";
   }

   //read csv head.put column name into  array
   private String[] importRecord(CsvReader reader,ArrayList<Integer> propNumArray,int recordType) throws IOException
   {

           reader.setUseComments(true);

           reader.readHeaders();

           String[] headers =  reader.getHeaders();

           for(int i=0;i<reader.getHeaderCount();i++)
           {
               if (this.propNumFromPropName(recordType,headers[i])==0)
                   continue;
               propNumArray.add(this.propNumFromPropName(recordType,headers[i]));
           }
           return headers;
   }

    public ArrayList<CPSCrop> importCropsAndVarieties( String fileName )
    {
        ArrayList<CPSCrop> crops = new ArrayList<CPSCrop>();
        ArrayList<Integer> propNumArray = new ArrayList<Integer>();
        CSV csv = new CSV();
        String[] headers;
        try
        {
            CsvReader reader = new CsvReader(fileName);
            headers = importRecord(reader,propNumArray,CPSDataModelConstants.RECORD_TYPE_CROP);
            while (reader.readRecord())
            {
                CPSCrop crop = new CPSCrop();
                for(int i=0;i<reader.getHeaderCount();i++)
                {
                    setCropsAndVarieties(crop,propNumArray.get(i),reader.getValues()[i]);
                }
                crops.add(crop);
            }
        }
        catch ( Exception e ) { e.printStackTrace(); }
        
        return crops;
    }

    private void setCropsAndVarieties(CPSCrop crop,final int INDEX,String value)
     {
         crop.set(INDEX,value);
     }

    public ArrayList<CPSPlanting> importCropPlan( String fileName )
    {
        ArrayList<CPSPlanting> plants = new ArrayList<CPSPlanting>();
        ArrayList<Integer> propNumArray = new ArrayList<Integer>();
        CSV csv = new CSV();
        String[] headers;
        try
        {
            CsvReader reader = new CsvReader(fileName);
            headers = importRecord(reader,propNumArray,CPSDataModelConstants.RECORD_TYPE_PLANTING);
            while (reader.readRecord())
            {
                CPSPlanting plant = new CPSPlanting();

                for(int i=0;i<reader.getHeaderCount();i++)
                {
                    setPlanting(plant,propNumArray.get(i),reader.getValues()[i]);
                }
                plants.add(plant);
            }

        }
        catch (Exception e) { e.printStackTrace(); }
        return plants;
    }

    private void setPlanting(CPSPlanting plant,final int INDEX,String value)
     {
       switch ( INDEX ) {
          
//          case CPSDataModelConstants.PROP_DATE_PLANT:
          case CPSDataModelConstants.PROP_DATE_PLANT_ACTUAL:
          case CPSDataModelConstants.PROP_DATE_PLANT_PLAN:
//          case CPSDataModelConstants.PROP_DATE_TP:
          case CPSDataModelConstants.PROP_DATE_TP_ACTUAL:
          case CPSDataModelConstants.PROP_DATE_TP_PLAN:
//          case CPSDataModelConstants.PROP_DATE_HARVEST:
          case CPSDataModelConstants.PROP_DATE_HARVEST_ACTUAL:
          case CPSDataModelConstants.PROP_DATE_HARVEST_PLAN:
             plant.set( INDEX, CPSDateValidator.simpleParse( value ) );
             return;
             
          default:
             plant.set( INDEX, value );
             return;
       }  
       
     }
    
}