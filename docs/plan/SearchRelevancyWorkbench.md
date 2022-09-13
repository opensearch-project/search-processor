# Search Relevancy Dashboard deliverables

This document represents a high level composition of tasks and architecture which will be taken to implement
the search relevancy workbench. 

##  Workbench Components

### Search Configuration
The 'Search Configuration' is the search planning and construction process that is taken prior of running the actual search.
This 'configuration' may include composition of sub elements and chaining of different search related sections such as
 - Query Rewrite 
 - Query Context/Scoping 
 - Result ranking 

For simplicity and standardization we will define the configuration processes as the construction of a chain of transformations, each transformation has its own purpose and 
agenda.

The only requirement here is to be able to chain together different transformations - meaning that the output of one transformation can be
chained as the input of the following transformation.


### Search Interface
The 'Search Interface' is the actual search process that is called using the previously created search config.
It includes the actual query that is given by the customer and possibly additional metadata related context about the tenant executing the query.

----

### User Interface
The next section describes the User Interface components that are driving the work flow of the workbench.

```text
[V-1]: referring to the first release of this feature  
[V-2]: referring to the following release of this feature  
```
   1. Create high level left-menu for Relevancy Workbench
   2. Create tabs for Search interface and Search Configs
      - ***Search Config Tab***
          1. Table list
        
        | Name  | Desc        | Index    | Tags | Modified       |
        |-------------|----------|------|----------------|--------|
        | testConf | sample conf | _test    | ...  | 01-Jan-2023    |
         
          2. Allow CRUD Operations button on the table list
          3. Create search config UI:
              1. metadata input:  name, tags, description
              2. index input:  index/index pattern
              3. Execution Chain Input: DSL Query with templates for End user query input
      
      4. ***Search Interface Tab***
          1. Table list
         
             | Name | Desc   | Tags | Index | Params              | Modified    | #Results |
             |------|--------|------|-------|---------------------|-------------|----------|
             | test | sample |      | _test | query:'hello world' | 02-Jan-2023 | 12       |
         
          3. Allow CRUD Operations button on the table list
          4. Create search interface UI:
              1. Modal pop-up with input-metadata for name, tags, description
              2. Redirect to empty search interface
          - [V-1] _Option for adding existing search configs via side panel_ [similar to operational panels]
          - [V-1] _Option to create new search configs (via redirect to search config creation page)_
          6. End user query/params input bar [store browser session]
              - key/value pairs for template input injection
          7. Results table facet [similar to events analytics]
          8. Field Selector facet [similar to events analytics]
          9. Document Flyout - 
             - ability to select a particular document from the result table and see complete json of the document [similar to events analytics]
      5. **Compare** - Ability to select 2 search configs for comparison in runtime
              - **Compare Search Configs**:
                  1. Click compare button
                  2. Flyout Select 2nd Search Config
                  3. Split the result facet in two
                  4. synced Field Selector facet
                  5. Double Document Flyout
              - **Compare Meta-Data**
                  1. check difference in DSL queries
                  2. check difference in (query) input params
              - **Compare live search** result with a saved result [V-2]
                  1. check difference between live result set and a golden set/previous saved result
          - Go back single view from compare
      6. **Save search meta-data** [saved explicitly by user]
         - Fields:
           - name
           - description
           - tagging
           - input-params
         _Data is saved in the plugin's index  [V-1]
         
         14. **Save search result configuration** (saved explicitly by user)  [V-2]
          - Fields:
              - name
              - description
              - tagging
              - Index or S3/external storage to save the results
              - numbers of results to be saved per search
              - input-params
      7. *Saved Searches Tab*
          1. UI tables 
              1. Saved Meta-data
                  - Generated DSL query
                  - timestamp
                  - name
                  - description
                  - tagging
                  - input-params
              2. Saved Results [V-2] [top n/200/500] _(new result-index -?  or global mapping index -?)_
                  - timestamp
                  - name
                  - description
                  - tagging
---
### Backend 
The next section describes the Backend components that are driving the work flow of the workbench.
   1. Plugin Index: “.opensearch-relevancy”
   
   - **Components**:
       1. Search Config
       2. Search interface
       3. Saved Search Meta-data
---
   - **Object Store**
       1. Supported index mapping for types:
          - search_config
          - saved_interface:
          - search_meta_data:
   - **API**
       1. Create objects 
          - Search Config Object
          - Search Interface Object
          - Search Results Meta-data Object
       2. Validate object
          - Search Config validation
          - Search Interface validation
       3. Search object 
           - by type
           - by id/ ids
           - by names
           - with sorted fields
           - form index/ max-Items
       4. Update object PUT
       5. Delete object  DELETE
     
   - **Security**
       1. User access check
       2. Tenant based check

---
### **Search Analytics Requirement** 

  - Users should be able to:
    - view aggregated analytics with data from all Search Apps.
    - filter the analytics view
      - by search app name, tags using PPL Query.
      - by time interval.
    - see total number of:
      - queries
      - total number of queries with no results
      - total result-clicks in the analytics
      - stats on ranks of clicks.
    - see graphs for:
      - total queries
      - queries without results per hour/day/month (based on time interval selected by user).
    - see tables of
      - top queries
      - top queries with no results.

