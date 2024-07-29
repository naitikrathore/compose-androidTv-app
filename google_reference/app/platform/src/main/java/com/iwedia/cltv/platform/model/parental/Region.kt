package com.iwedia.cltv.platform.model.parental

enum class Region {
    CN,
    US,
    SA,
    EU,
    PA;

    companion object {
        fun getRegionList() : List<Region>{
            return listOf(CN,US,SA,EU,PA)
        }
        fun getCountryList(region: Region) : List<String>{
            // for more reference tv_configuration.json
            return when (region) {
                //AS - Asia
                CN ->{
                    listOf("CN","HK")
                }
                //NA - North America
                US ->{
                    listOf("US","KR","CA","MX")
                }
                //SA - South America
                SA ->{
                    listOf("AR","BO","BW","BR","CR","CL","EC","GT","HN","JP","MV","NI","PY","PH","PE","LK","UY","VE","AO")
                }
                //EU - Europe
                EU ->{
                    listOf("AT","BE","CH","CZ","DE","DK","ES","FI","FR","GB","IT","LU","NL","NO","SE","BG","HR","GR","HU","IE","PL","PT","RO","RU","RS","SK","SI","TR","EE","UA")
                }
                //PA - Pacific Asia
                PA ->{
                    listOf("ID", "MY", "NZ", "AU", "SG", "TH", "VN", "MM", "TW", "LV", "LT", "IN", "AE", "GH")
                }
            }
        }
    }

}