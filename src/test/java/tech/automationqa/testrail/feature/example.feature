Feature: sample karate test script
  for help, see: https://github.com/intuit/karate/wiki/IDE-Support

  Background:
    * url 'https://api.nytimes.com/svc/'

  @this
  Scenario: Validate book names schema
    Given path 'books/v3/lists/names.json'
    And param api-key = "JUE8kjb2GFKgnT8jKH4s41r4GR0IpCyE"
    When method get
    Then status 200
    And match response.status == "OK"
    And match response.copyright =='#string'
    And match response.num_results =='#number'
    And match response.results =='#[59]'
    And match each response.results ==
    """
       {
       list_name: '#string',
       display_name: '#string',
       list_name_encoded: '#string',
       oldest_published_date: '#string',
       newest_published_date: '#string',
       updated: '#string'
       }
    """

  Scenario: Get Best Sellers list names.
    * def resultSchema = { list_name: '#string',  display_name: '#string',  list_name_encoded: '#string',  oldest_published_date: '#string',  newest_published_date: '#string',  updated: '#string'}

    Given path 'books/v3/lists/names.json'
    And param api-key = "JUE8kjb2GFKgnT8jKH4s41r4GR0IpCyE"
    When method get
    Then status 200
    And match response ==
    """
     {
     status:'OK',
     copyright: '#string',
     num_results: '#number',
     results:'#[] resultSchema'
     }
    """

    * def names = get response.results[*].list_name
    And print names