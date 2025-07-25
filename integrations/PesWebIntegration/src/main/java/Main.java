import service.DogExporter;

public class Main {
  public static void main(String[] args) throws Exception {
    // will write all dogs into dogs.json in your working dir
    DogExporter.exportAllDogs("dogs.json");
  }
}
