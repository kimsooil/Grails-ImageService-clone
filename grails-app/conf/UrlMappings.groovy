class UrlMappings {

	static mappings = {
    "/$controller" (controller:"view", action: "showError", parseRequest: true){ }
    "/$controller/$serviceName" (controller:"view", action: "showError", parseRequest: true){ }
    "/$controller/$serviceName/$requestFile" (controller:"view", action:"viewImage", parseRequest: true){ }
    "/$controller/$serviceName/$width/$requestFile" (controller:"view", action: "showError", parseRequest: true){ }
    "/$controller/$serviceName/$width/$height/$requestFile" (controller:"view", action: "resizeImage", parseRequest: true){ }
		"/"(view:"/index")
		"500"(view:'/error')
	}
}