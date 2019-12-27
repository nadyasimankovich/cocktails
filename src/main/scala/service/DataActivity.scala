package service

import cocktail.CocktailsDataService

class DataActivity(cocktailsDataService: CocktailsDataService) {

  def update: Runnable = new Runnable {
    override def run(): Unit = {
      for {
        _ <- cocktailsDataService.reload()
      } yield {
      }
    }
  }
}