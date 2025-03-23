# Spring

### Поднятие контекста 
- BeanDefinitionReader обнаруживает бины и создает _BeanDefinitions_ - декларация класса (название, инит метод, проперти)
- BeanFactoryPostProcessor позволяет настроить _BeanDefinition_ и саму _BeanFactory_ перед работой
- Создается _BeanFactory_
- Внедряются зависимости
- Отрабатывает _BeanPostProcessor#BeforeInicialisation_ 
- Отрабатывает _PostConstruct_
- Отрабатывает _BeanPostProcessor#AfterInicialisation_
- Узнаем о том что поднят контекст по событию из _ApplicationContextEvent_


### Aspect
### Proxy
### Async
### Transaction
