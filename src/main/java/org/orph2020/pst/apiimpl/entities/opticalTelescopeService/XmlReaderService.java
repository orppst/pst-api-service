package org.orph2020.pst.apiimpl.entities.opticalTelescopeService;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Logger;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

/**
 * business logic for the telescope config DB
 */
public class XmlReaderService {

   private static final Logger LOGGER =
       Logger.getLogger("TelescopeConfigService");

   // constant environmental value for where telescope data is located.
   private static final String TELESCOPE_CONFIG_LOCATION =
       "POLARIS_TELESCOPE_CONFIG_LOCATION";

   // xml extension.
   private static final String XML = ".xml";

   // configuration xml name
   private static final String TELESCOPE_ATTR = "telescope";

   // option xml tag
   private static final String OPTION_CONFIG = "option";

   // options xml tag
   private static final String OPTIONS_CONFIG = "options";

   // option list xml tag
   private static final String OPTION_LIST = "optionlist";

   // option for text field xml tag
   private static final String OPTION_TEXT = "text";

   // option for boolean field.
   private static final String BOOLEAN_TEXT = "boolean";

   // top level point where the telescope config lives.
   private static final String TELESCOPE_CONFIG = "telescopeConfigurations";
   
   // store of telescopes.
   private HashMap<String, Telescope> telescopes;

   /**
    * default public interface, forces to utilise the environment variable for
    * the location of the xml store.
    */
   public void read() {
      this.read(null);
   }

   /**
    * public interface (mainly for testing) which allows the xml path to be
    * changed.
    * @param telescopeXMLPath: param for the environment setting.
    */
   public void read(String telescopeXMLPath) {
      LOGGER.info("initializing Database");

      // get class path to the location where the telescopes xml is located
      if (telescopeXMLPath == null) {
         telescopeXMLPath = System.getenv(TELESCOPE_CONFIG_LOCATION);
      }

      if(telescopeXMLPath == null) {
         LOGGER.log(SEVERE, "no environment variable was found. Exiting");
         return;
      } else {
         LOGGER.log(INFO,
             String.format("environment variable is %s", telescopeXMLPath));
      }

      // filter out all noise in the repo to locate just the xml.
      ArrayList<File> xmlFiles = this.findAllXMLs(telescopeXMLPath);

      Schema schema;
      try {
         SchemaFactory sf = SchemaFactory.newInstance(
             XMLConstants.W3C_XML_SCHEMA_NS_URI);
         schema = sf.newSchema(
             new File(telescopeXMLPath + "schema.xsd"));
      } catch (SAXException e) {
         LOGGER.log(SEVERE, String.format(
             "received a SAXException with content %s",  e.getMessage()));
         return;
      }


      // iterate over each xml and read in the values.
      telescopes = new HashMap<>();
      for(File xmlFile: xmlFiles) {
         telescopes.putAll(this.processXML(xmlFile, schema));
      }


      // print out a copy of the telescope for debugging.
      LOGGER.log(INFO, "The populated telescopes are as follows:");
      for(Telescope t: telescopes.values()) {
         LOGGER.log(INFO, t.toString());
      }
   }

   /**
    * reads in the xml and traverses it. building java objects for grouping.
    *
    * @param xml: the xml file.
    * @return the list of telescopes contained within the xml file.
    */
   private HashMap<String, Telescope> processXML(File xml, Schema schema) {
      HashMap<String, Telescope> telescopes = new HashMap<>();

      // Create a DocumentBuilderFactory instance
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setSchema(schema);
      factory.setIgnoringElementContentWhitespace(true);

      try {
         // Create a DocumentBuilder instance
         DocumentBuilder builder = factory.newDocumentBuilder();

         // Parse the XML document
         Document document = builder.parse(xml);
         document.getDocumentElement().normalize();

         // Get the root element
         Element root = document.getDocumentElement();

         // get telescope name and create new telescope object from it.
         Attr telescopeNameAttr = root.getAttributeNode(TELESCOPE_ATTR);
         Telescope telescope = new Telescope(telescopeNameAttr.getValue());
         telescopes.put(telescope.getName(), telescope);

         // try getting the telescope configurations and the tag for
         // instrument config.
         String instrumentConfigName = this.getInstrumentConfig(
             document, telescope);

         // try getting the separate instruments and their fields.
         this.getInstruments(instrumentConfigName, document, telescope);

         // populate options states.
         this.populateOptions(document, telescope, instrumentConfigName);

         // clean up any madness
         this.cleanMadness(telescope);

      } catch (javax.xml.parsers.ParserConfigurationException e) {
         LOGGER.log(SEVERE, String.format(
             "received a ParserConfigurationException with content %s",
             e.getMessage()));
      } catch (org.xml.sax.SAXException e) {
         LOGGER.log(SEVERE, String.format(
             "received a SAXException with content %s", e.getMessage()));
      } catch (java.io.IOException e) {
         LOGGER.log(SEVERE, String.format(
             "received a IOException with content %s", e.getMessage()));
      } catch (Exception e) {
         LOGGER.log(SEVERE, String.format(
             "received an exception with content %s", e.getMessage()));
      }
      return telescopes;
   }

   /**
    * removes messy elements. So far these are drop-downs with no list elements.
    *
    * @param telescope: the telescope to remove broken elements from.
    */
   private void cleanMadness(Telescope telescope) {
      for (Instrument instrument: telescope.getInstruments().values()) {

         // find removals. store in new array due to incremental removal.
         ArrayList<String> removals = new ArrayList<>();
         for (String fieldKey: instrument.getElements().keySet()) {
            Field field = instrument.getElements().get(fieldKey);
            if (field.getType().equals(Field.TYPES.LIST) &&
                  field.getValues().size() == 0) {
               removals.add(fieldKey);
            }
         }

         // remove all found removals.
         for (String removalKey: removals) {
            LOGGER.log(SEVERE, String.format(
                "removing telescopes %s, instruments %s, %s as its got no" +
                    " entries.",
                telescope.getName(), instrument.getName(), removalKey));
            instrument.getElements().remove(removalKey);
         }
      }
   }

   /**
    * populates the options from the xml to the telescope instruments.
    *
    * @param document: the xml doc root element.
    * @param telescope: the data object.
    * @param instrumentConfigName: the tag for the instrument configs.
    */
   private void populateOptions(
         Document document, Telescope telescope, String instrumentConfigName)
         throws Exception {
      NodeList options = document.getElementsByTagName(OPTIONS_CONFIG);
      for (int optionsIndex=0; optionsIndex < options.getLength();
           optionsIndex++) {
         Element optionsElement = (Element) options.item(optionsIndex);
         String elementName =
             optionsElement.getAttributes().item(0).getNodeValue();

         // only look for options not yet processed.
         if (!elementName.equals(instrumentConfigName) &&
             !elementName.equals(TELESCOPE_CONFIG)) {

            // get the options and iterate over them.
            Element optionElement = (Element) options.item(optionsIndex);
            NodeList optionElementList =
                optionElement.getElementsByTagName(OPTION_CONFIG);
            for (int optionElementIndex=0;
                 optionElementIndex < optionElementList.getLength();
                 optionElementIndex++) {

               // locate which element this is targeting.
               Element element = (Element) optionElementList.item(
                   optionElementIndex);
               String elementOption =
                   element.getFirstChild().getFirstChild().getNodeValue();

               // locate which instruments utilise this element.
               Element dependenciesList = (Element)
                   element.getFirstChild().getNextSibling();
               NodeList instrumentsTargeted =
                   dependenciesList.getElementsByTagName("select");
               for (int instrumentsTargetedIndex=0;
                    instrumentsTargetedIndex < instrumentsTargeted.getLength();
                    instrumentsTargetedIndex++) {
                  String instrumentTargeted =
                      instrumentsTargeted.item(instrumentsTargetedIndex).
                          getFirstChild().getFirstChild().getNodeValue();

                  // add the option to the element in the telescope instrument.
                  Instrument instrument =
                      telescope.getInstrument(instrumentTargeted);
                  if (instrument == null) {
                     LOGGER.log(WARNING, String.format(
                         "Telescope %s does not contain instrument %s, and so" +
                             " element %s will not be added. If you want the " +
                             "instrument to exist, please populate it within " +
                             "the instrumentConfigurations config options.",
                         telescope.getName(), instrumentTargeted, elementName));
                  } else {
                     instrument.addOption(elementName, elementOption);
                  }
               }
            }
         }
      }
   }


   /**
    * gets the instruments.
    *
    * @param instrumentConfigName: the tag for the instrument configs.
    * @param document: the xml document.
    * @param telescope: the telescope data object to populate with instruments.
    */
   private void getInstruments(String instrumentConfigName, Document document,
                               Telescope telescope) {
      NodeList options = document.getElementsByTagName(OPTIONS_CONFIG);
      for (int optionsIndex=0; optionsIndex < options.getLength();
           optionsIndex++) {
         Element optionsElement = (Element) options.item(optionsIndex);

         // is it a telescope config
         if (optionsElement.getAttributes().item(0).getNodeValue().equals(
               instrumentConfigName)) {
            NodeList instrumentOptions = optionsElement.getElementsByTagName(
                OPTION_CONFIG);

            // cycle through instruments
            for (int instrumentOptionIndex =0;
                 instrumentOptionIndex < instrumentOptions.getLength();
                 instrumentOptionIndex++) {

               // find instrument name and build the data object.
               Element instrumentOption =
                   (Element) instrumentOptions.item(instrumentOptionIndex);
               String instrumentName = instrumentOption.getFirstChild().
                   getFirstChild().getNodeValue();
               Instrument instrument = new Instrument(instrumentName);
               telescope.addInstrument(instrument);

               // add elements of instrument
               NodeList elementList =
                   instrumentOption.getElementsByTagName(OPTION_LIST);
               extractInstrumentNames(
                   instrument, elementList, Field.TYPES.LIST);

               elementList =
                   instrumentOption.getElementsByTagName(OPTION_TEXT);
               extractInstrumentNames(
                   instrument, elementList, Field.TYPES.TEXT);

               elementList =
                   instrumentOption.getElementsByTagName(BOOLEAN_TEXT);
               extractInstrumentNames(
                   instrument, elementList, Field.TYPES.BOOLEAN);

            }
         }
      }
   }

   /**
    * extracts the instrument names and adds them to the instrument.
    *
    * @param instrument: the instrument to add the names to.
    * @param elementList: the xml element list containing the values.
    * @param type: int flag, 0 = option list, 1 = text, 2 = boolean.
    */
   private void extractInstrumentNames(
         Instrument instrument, NodeList elementList, Field.TYPES type) {
      for (int instrumentElementIndex =0;
           instrumentElementIndex < elementList.getLength();
           instrumentElementIndex++) {
         NamedNodeMap elementOptions =
             elementList.item(instrumentElementIndex).getAttributes();
         for (int attributeIndex =0;
              attributeIndex < elementOptions.getLength();
              attributeIndex++) {
            instrument.addElement(
                elementOptions.item(attributeIndex).getNodeValue(),
                type);
         }
      }
   }

   /**
    * locates the instrument config tag from the telescope config.
    * @param document: the xml document
    * @param telescope: the telescope object.
    * @return the tag for the instrument configs.
    */
   private String getInstrumentConfig(Document document, Telescope telescope) {
      NodeList options = document.getElementsByTagName(OPTIONS_CONFIG);
      for (int optionsIndex=0; optionsIndex < options.getLength();
           optionsIndex++) {
         Element optionsElement = (Element) options.item(optionsIndex);

         // is it a telescope config
         if(optionsElement.getAttributes().item(0).getNodeValue().equals(
               TELESCOPE_CONFIG)) {
            Element telescopeOption =
                (Element) optionsElement.getElementsByTagName(
                    OPTION_CONFIG).item(0);

            // verify name
            String telescopeName =
                telescopeOption.getFirstChild().getFirstChild().getNodeValue();
            if (!telescopeName.equals(telescope.getName())) {
               LOGGER.log(WARNING, "the telescope names dont match!");
            }

            // get subfields (locate the tag for the instrument configs)
            NamedNodeMap optionList = telescopeOption.getFirstChild().
                getNextSibling().getFirstChild().getAttributes();
            return optionList.item(0).getNodeValue();
         }
      }
      return null;
   }

   /**
    * pulls the xml files out, ina  repo, there could be things like
    * .gitignore etc. to avoid.
    *
    * @param path: the folder path.
    * @return the list of xml files.
    */
   private ArrayList<File> findAllXMLs(String path) {
      ArrayList<File> xmlFiles = new ArrayList<>();
      File folder = new File(path);

      for (final File file : Objects.requireNonNull(folder.listFiles())) {
         if (file.isFile() && getFileExtension(file).equals(XML)) {
            xmlFiles.add(file);
         }
      }
      return xmlFiles;
   }

   /**
    * gets the extension of the file.
    *
    * @param file: the file to get the extension of.
    * @return the file extension.
    */
   private String getFileExtension(File file) {
      String name = file.getName();
      int lastIndexOf = name.lastIndexOf(".");
      if (lastIndexOf == -1) {
         return ""; // empty extension
      }
      return name.substring(lastIndexOf);
   }
   
   public HashMap<String, Telescope> getTelescopes() {
      return this.telescopes;
   }
}
