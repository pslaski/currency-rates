# Currency Rates

Simple currency rates application which is using Fixer.io API.

## Run
Fixer.io access key is needed to start the application and can be provided as a configuration value for `currency-rates.fixer.access-key` or as an environment variable for `FIXER_ACCESS_KEY`.

Then start application by running:

`sbt run`

Http server will start at `localhost:9000`

## Endpoints

Application exposes three endpoints:

#### Rates

GET `/rates` - it returns list of rates from given currency to other currencies

##### parameters:

- `base` - required - symbol of base currency 
- `target` - optional - symbol of target currency. If it exists, endpoint returns only rate for that currency
- `timestamp` - optional - historical date. If it exists, endpoint returns historical data for given day

##### examples:

`/rates?base=USD` - returns list of latest rates from USD to other currencies

`/rates?base=USD&target=EUR&timestamp=2016-05-01T14:34:46Z` - returns rate from USD to EUR at 2016-05-01 day

#### Registering Observer

POST `/publishing/<currency_symbol>/<time_interval_in_seconds>` - it registers observer, which will check given currency's rates with given time interval and send changed rates information to webhook address.

Default webhook is `http://localhost:7091/webhooks`, but it could be changed by providing `currency-rates.publishing.webhook-uri` value or as an environment variable for `WEBHOOK_URI`.
It can be only one observer for given currency in time.

##### example:

`/publishing/USD/10` - will check USD rates every 10 seconds and if needed send newest rates data to the webhook

#### Unregistering Observer

DELETE `/publishing/<currency_symbol>` - it unregisters observer for given currency

##### example:

`/publishing/USD` - will stop checking USD rates