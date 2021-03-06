angular.module('st-common-module', ['ngCookies','angularLocalStorage'])

.controller("PagingCtrl", ["$scope","$location",function($scope, $location) {
    init();
    function init() {
        $scope.filters.page = parseInt($scope.filters.page) || 1;
    }
    $scope.$watch('filters.page', function(v) {
        $scope.currentPage = parseInt($scope.filters.page) || 1;
        window.scrollTo(0, 0);
    });
}]).provider('CommonFunctions', function() {
    var self = this;

    this.wordMappings = {};

    this.setWordMappings = function(wordMappings) {
        this.wordMappings = wordMappings;
    }

    this.$get = ['$rootScope','$location','$timeout',function($rootScope, $location, $timeout) {
        $rootScope.alerts = [];

        $rootScope.closeAlert = function(index) {
            $rootScope.alerts.splice(index, 1);
        }

        return {
            pushAlert : function(type, message) {
                var length = $rootScope.alerts.push({
                    type : type,
                    msg : message
                });
                $timeout(function() {
                    $rootScope.alerts.splice(length - 1);
                }, 5000);
            },
            pushAlertByType : function(type) {
                switch (type) {
                case "login":
                    this.pushAlert('danger', "Musisz się najpierw zalogować.");
                    break;
                case "accessDenied":
                    this.pushAlert('danger', "Nie masz wystarczających uprawnień.");
                    break;
                case "badRequest":
                    this.pushAlert('danger', "Ups, coś poszło nie tak. Proszę, postępuj zgodnie z podpowiedziami");
                    break;
                }
            },
            getPostHeader : function() {
                return {
                    headers : {
                        "Content-Type" : "application/x-www-form-urlencoded"
                    }
                };
            },
            translateWord : function(word) {
                return self.wordMappings[word];
            },
            translateUrl : function(url) {
                var parts = url.split('/');
                if (parts[0] == '')
                    delete parts[0];
                var translated = "/";
                var translateUrlObject = this;
                angular.forEach(parts, function(value) {
                    var word = translateUrlObject.translateWord(value);
                    if (word == "") {
                        translated += word;
                    } else {
                        translated += word || value;
                        translated += "/";
                    }
                });
                if (translated.length > 1)
                    translated = translated.slice(0, translated.length - 1);
                return translated;
            },
            getTranslatedUrl : function() {
                return this.translateUrl($location.path());
            }
        }
    }];
}).factory('ErrorFactory', function() {
    this.errorMessages = {
        pattern : {
            def : "Niewłaściwe znaki"
        },
        minlength : {
            def : 'Min {{data-ng-minlength}} znaków'
        },
        maxlength : {
            def : 'Max {{data-ng-maxlength}} znaków'
        },
        fieldMatch : {
            def : 'Pola nie są takie same'
        },
        email : {
            def : 'Niepoprawny email'
        },
        dateValid : {
            def : 'Niepoprawna data'
        },
        min : {
            def : 'Podana liczba musi być większa od {{data-min}}'
        },
        max : {
            def : 'Podana liczba musi być mniejsza od {{data-max}}'
        }
    };
    this.getErrorMessages = function(customErrorMessages) {
        return $.extend(true, this.errorMessages, customErrorMessages);
    };
    return this;
}).directive('stTimeoutTooltip', ['$animator',"$timeout",function($animator, $timeout) {
    return {
        restrict : 'A',
        link : function(scope, elem, attrs) {
            // var split = attrs.stOn.split('.');
            // var newTarget = scope;
            // for (var i = 0; i < split.length; i++) {
            // newTarget = newTarget[split[i]];
            // }
            // // var variableHolder = attrs.stOn.substring(0,
            // attrs.stOn.indexOf("."));
            // // var variable = attrs.stOn.substring(attrs.stOn.indexOf(".") +
            // 1);
            // scope.$watch(attrs.stOn, function(val) {
            // if (val) {
            // elem.addClass("in");
            // $timeout(function() {
            // newTarget = false;
            // }, attrs.timeout);
            // } else
            // elem.removeClass("in");
            // });
            scope.$on(attrs.stOn, function() {
                elem.addClass("in");
                $timeout(function() {
                    elem.removeClass("in");
                }, attrs.timeout);
            });
        }
    };
}])
//.directive('stHref', function() {
//    return {
//        restrict : 'A',
//        link : function(scope, element, attrs) {
//            attrs.$observe('stHref', function(value) {
//                element.attr('href', value.replace(/ /g, '-'));
//            });
//        }
//    };
//})
.directive('stNewWindow', function() {
    return {
        restrict : 'A',
        link : function(scope, elem, attrs) {
            var width = attrs.width ? attrs.width : 650;
            var height = attrs.height ? attrs.height : 350;
            elem.bind('click', function(e) {
                e.preventDefault();
                var x = screen.width / 2 - width / 2;
                var y = screen.height / 2 - height / 2;
                window.open(attrs.href || attrs.stHref, 'newwindow', "width=" + width + ", height=" + height + ", left=" + x + ", top=" + y);
            });
        }
    };
}).provider('MetatagsCreator', function() {
    var metatags = {};
    this.setDefaultMetatags = function(tags) {
        metatags = tags;
    };
    this.$get = ['$rootScope',function($rootScope) {
        $rootScope.metatags = metatags;
    }];
});

(function() {
    var createDirective, module, pluginName, _i, _len, _ref;

    module = angular.module('FacebookPluginDirectives', []);

    createDirective = function(name) {
        return module.directive(name, ['$timeout',function($timeout) {
            return {
                restrict : 'C',
                compile : function(scope, element, attributes) {
                    return {
                        post : function postLink(scope, iElement, iAttrs, controller) {
                            scope.$watch('ad.id', function() {
                                $timeout(function() {
                                    return typeof FB !== "undefined" && FB !== null ? FB.XFBML.parse(iElement.parent()[0]) : void 0;
                                }, 50, false);
                                // return typeof FB !== "undefined" && FB !==
                                // null ? FB.XFBML.parse(iElement.parent()[0]) :
                                // void 0;
                            });
                        }
                    };
                }
            };
        }]);
    };

    _ref = ['fbActivity','fbComments','fbFacepile','fbLike','fbLikeBox','fbLiveStream','fbLoginButton','fbName','fbProfilePic','fbRecommendations'];
    for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        pluginName = _ref[_i];
        createDirective(pluginName);
    }

}).call(this);