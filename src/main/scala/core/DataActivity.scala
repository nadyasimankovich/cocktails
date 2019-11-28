package core

class DataActivity(cocktailsDataService: CocktailsDataService) {

  def update: Runnable = new Runnable {
    override def run(): Unit = {
      for {
        images <- cocktailsDataService.getAllImages
      } yield {
      }
    }
  }
}