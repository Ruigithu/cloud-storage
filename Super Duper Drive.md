# Super Duper Drive

This is a Drive application, which an save and download various kinds of files of the user.

This application will combine React framework as the frontend, and java spring boot as the backend technique.This passage will record the whole process pf developing this application. And the regarding skill knowledges will attached to the process time to time, then I can review them on the bus to shcool or get back. I asked Claude AI to help  me to fix the bugs or design the website itself, and then I was shocked by its competent  on developing, and I know that maybe I'll never learn as much as it did, but I also don't want to totally rely on AI, cause I really want to learn something. Today I could not help but feeling down all the time, because I was afraid that I could not make it and will never find a job. And even now I cannot say that I'm sure I've gone through this period that full of fears. But the things that I learned won't cheat on me, they'll show up on my mind, on my confidence, and let me know what I really want to do in the future. My life journey is long, so please don't stop. I believe you'll make the proper decissions for you and everything will be solved. Keep walking.

## Git Hub

> Git Hub is a really good tool to host our code. And there I want to introduce the process that how will we use it during the developing process.

+ create a repo on your git hub account, there are three files that you need to set before you start developing it.
  
  <img src="file:///D:/my%20experience%20of%20developing%20my%20first%20application/one-git-three-files.png" title="" alt="image" width="373">
  
  + **README**   - which is used to describe your application
  
  + **.gitignore** -to tell git hub what kind of files that you don't want want to push to your git repo in case that the leak of imortant information. My project mainly use java language, so I choose java as default.
  
  + **license** -the open source protocal, I choose MIT, known as one of the most tolerant protocal, which only requires keeping copyright notice and permission notice. And I was told that React also use this protocal.

+ after you create your repo, it will be good for you to create another branch (develop) beside of main branch and set it as default. Every time you commit your new developing progress, it'll first be updated into the develop branch, and then you can pull request them to themain branch as the stable version for lanuching.

+ The you can clone the repo to your local repo, the tool you need to use is  ***git bash***
  
  + ```bash
    cd "your project file"
    git clone "your github repo url"
    ```

+ after you finish the some develop process
  
  + ```bash
    if you don't have the develop branch，
    you need to first create and track the remote develop branch：
    git checkout -b develop origin/develop
    
    git add . store the file to the temporary storage area
    git commit -m "commit your update message for record"
    
    if there are updates in the remote repo，pull the remote first（not fast/ behind）
    git pull origin develop
    
    then submit your repo
    git push origin develop
    ```

## Construct the project

### Decide your technique stack

> I want to use react and spring boot to develop my application. First I need to create the spring project in IDE, and then in the root category of the my project, I need to create the React project. After I finish all the developing, I can use maven to copy the frontend assets and backend by  run  `mvn clean install` 

## The front end

### React

+ There are some questions that I think I'll answer later, because I don't know the precise answer but I'm thinking I may use it in the interview.
  
  + ```markdown
     Why will we use the React framework?
    ```

+ I think I won't repeat the process of creating the initial java project because I'm so familiar with it. And I think I need to review more times about REACT.

+ creating a react project. In the terminal,automaticaaly create the entire structure
  
  + ```bash
    npx create-react-app frontend  
    ```
- then go to package.json to check whether the version of react is the latest and the most stable
  
  - ```json
    {
      "dependencies": {
        "react": "^18.2.0",
        "react-dom": "^18.2.0"
        // ... other dependencies
      }
    }
    ```
+ there are also another way to create the react app, which is more annoying, because when you first run `npm init -y`,it will only generate the ***package.json*** file, and you have to install other dependencies yourself. If I need it in the future, just ask AI to help you.
  
  #### Structure

+ Then I'll introduce the basic react project catalog
  
  + ```json
    src/
      ├── assets/          # for static assets
      │   ├── images/      # images
      │   └── styles/      # css file
      │
      ├── components/      # can be used repeatly
      │   ├── Button/
      │   │   ├── Button.jsx
      │   │   └── Button.css
      │   └── Header/
      │       ├── Header.jsx
      │       └── Header.css
      │
      ├── pages/          # page components
      │   ├── Home/
      │   │   ├── Home.jsx
      │   │   └── Home.css
      │   └── Login/
      │       ├── Login.jsx
      │       └── Login.css
      ├── context/        # React Context
      │   └── AuthContext.js
      │
      ├── App.jsx         # root component, you can config the routes there
      └── index.jsx       # the entry,get elements from the index.html,
                             and render it.             
    
    public/
      ├── index.html      # HTML template
      └── favicon.ico     # the icon of the website
    
    package.json          # dependencies and scripts, where you can run react project
    README.md            # project specification
    .gitignore           # Git ignore file
    ```

+ I'll then introduce the very basic react developing for you.
  
  + there are three files that browser will load first
    
    + ```json
      → browser will first load index.html 
      → index.html loads index.jsx 
      → index.jsx renders App.jsx 
      → App.jsx renders other components
      ```

+ the lowest components are the page`.jsx` files
  
  + the components code style like this
    
    + ```javascript
      const componentName => (){
      return(/*the javascript and html code can be written here*/);
      }
      export default componentName;
      
      /*Then this component can be used in other files*/
      import componentName from 'path';
      ```

+ So my coding style is write page components independently and linked with css file
  
  #### Routes

+ then config their components jsx routes in the App.jsx
  
  + ```javascript
    import { BrowserRouter, Routes, Route } from 'react-router-dom';
    import Home from "./pages/Home/home";
    
     function App() {
        return(
         <BrowserRouter>
             <Routes>
                 <Route path="/" element={<Home />} />
             </Routes>
         </BrowserRouter>
        );
     }
    
    export default App;
    ```

+ when it comes to the routers, you need to first install the dependency
  
  + ```bash
    npm install react-router-dom
    ```
- there are the function of the router tags
  
  - ```json
    BrowserRouter: the container
    Routes: the rules of route
    Route: the specific route rules
    ```
+ A very good templates of the route config, now I didn't uderstand it very well
  
  + ```javascript
    import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom';
    import { useAuth } from './hooks/useAuth';
    
    // 布局组件
    const Layout = () => {
      return (
        <div>
          <header>
            {/* nav component */}
            <nav>
              <Link to="/">首页</Link>
              <Link to="/about">关于</Link>
              <Link to="/dashboard">控制台</Link>
            </nav>
          </header>
    
          {/* child route component */}
          <main>
            <Outlet />
          </main>
    
          <footer>© 2025</footer>
        </div>
      );
    };
    
    // route protect compontent
    const ProtectedRoute = ({ children }) => {
      const { isAuthenticated } = useAuth();
    
      if (!isAuthenticated) {
        // unlogin condition and renav to the login page
        return <Navigate to="/login" replace />;
      }
    
      return children;
    };
    
    // 路由配置
    const App = () => {
      return (
        <BrowserRouter>
          <Routes>
            {/* public route */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
    
            {/* the route that used layout component */}
            <Route element={<Layout />}>
              {/* homepage */}
              <Route path="/" element={<HomePage />} />
    
              {/* the route needs protection */}
              <Route
                path="/dashboard"
                element={
                  <ProtectedRoute>
                    <DashboardPage />
                  </ProtectedRoute>
                }/>
    
              {/* 404页面 */}
              <Route path="*" element={<NotFoundPage />} />
            </Route>
          </Routes>
        </BrowserRouter>
      );
    };
    ```

#### Single Page application

+ But on the app.jsx file, there will be many routes, and it seems they'll all be rendered on the index.html，why they won't conflict

```json
1.when user click something which nav to another url
2.the react router will capture this change
3.then it will match the url to the app.jsx
4.if they are matched successfully
5.then the content in the root div will change
6.so only the component in the root div will change
```

#### The root div

- After we explain the app.jsx, then we need to explain the index.js and index.html

- the `index.html` will be very easy, what will be included is <div id="root">

- and then, you need to get this element and render it in the `index.js`
  
  - ```javascript
    // get root element
    const rootElement = document.getElementById('root');
    const root = ReactDOM.createRoot(rootElement); // use createRoot to create root instance
    
    // render  application
    root.render(
        <React.StrictMode>
           <App />
        </React.StrictMode>
    );
    ```

##### problem - solve

+ how to render the image

```javascript
import driveIcon from './assets/images/cloudversify-brands-solid.svg';

function App() {
  return (
    <img src={driveIcon} alt="drive-icon" />
  );
}
```

#### The frontend hompage design

+ When I first learned the html,css, I found there are lots of  tags and attributes to use, and I was afraid that I couldn't remember them, but after I designed the basic home page, I found that searching the doc  is more important.

#### Login  Page

> In the login page, we need to finish three main tasks. 

- create the login form html, and let user input their username and password,and pu thenm in the return part

- create the variables to store the username and password

- create the function deal with the input data
  
  - it should be async function
  
  - it should have fetch funtion to request to the backend api, and get the response
  
  - if the response is ok, then it should redirect to the home page

```jsx
import {useState} from "react";
import {useNavigate} from "react-router-dom";
import loginCss from "./login.css"

 function Login(){
     const[username,setUsername]=useState('');
     const[password,setPassword]=useState('');
     const navigate =useNavigate();

     const handleSubmit = async (e)=>{
//form tag has the default action like 
         e.preventDefault();

// URLSearchParams is a inner web api,it is kind of using the key-value 
//form to store the data
//example:{username: "Alice"
//         password: "12345"}

         const formData = new URLSearchParams();
         formData.append('username', username.trim());
         formData.append('password', password.trim());

         try {
             const response = await fetch('http://localhost:8080/login', {
//post method will not concat senitive data in the url
                 method: 'POST',
                 headers: {
 // Spring Security received "x-www-form-urlencoded" format by default 
                     'Content-Type': 'application/x-www-form-urlencoded',
                 },
//tranform it to the format of "username=Alice&password=12345"
                 body: formData.toString(),
                 credentials: 'include' // include cookies
             });

             //get the authentication response
             if (response.ok){
                 const data = await response.json();
                 localStorage.setItem("token","");
//if you login successfully, you'll nav to home page
                 navigate("/home")
             }

         }catch (error){
             console.error('Login failed:', error);
         }

     }


     return(
         <div className="login-container">
             <form className="form-container" onSubmit={handleSubmit}>
                 <label id="username"> username:
                     <br/>
                     <input className="signin-input"
                            type="text"
//when we bind input value to the const username,
// ensuring that the content of the input field is synchronized with username. 
//When the user types in the input field, 
//the onChange event updates the value of username, and in turn, 
//the update of username causes the content of the input field to be updated.
                            value={username}
                            onChange={
                                (e) => setUsername(e.target.value)
                            }/>
                 </label>
                 <label>password:
                     <br/>
                     <input className="signin-input" id="password"
                            type="password"
                            value={password}
                            onChange={
                                (e) => setPassword(e.target.value)
                            }/>
                 </label>
                 <button className="signin-submit" type="submit">Sign in</button>
                 <br/>
                 <div><a href="/signup">not have an account?click here to signup</a></div>
             </form>
         </div>
     );

 }
export  default Login;
```

> when we have not prepared for the backend api and database, we can use the follow test code:

```jsx
 const handleSubmit = async (e)=>{
         e.preventDefault();
         // mock login
         try {
             // mock API calling delay
             await new Promise(resolve => setTimeout(resolve, 1000));

             if (username === 'test' && password === 'test') {
                 console.log('successfully login');
                 localStorage.setItem("token", "test-token");
                 navigate("/home");
             } else {
                 alert('failed to login：invaild username or password');
             }
         } catch (error) {
             console.error('Login failed:', error);
             alert('failed to login: please try it later');
         }
```

### Connect the backend and the frontend By config cors

- ***config cors in config file***
  
  ```java
  package com.ruipeng.cloudstorage.config.cors;
  
  import org.springframework.context.annotation.Bean;
  import org.springframework.context.annotation.Configuration;
  import org.springframework.web.cors.CorsConfiguration;
  import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
  import org.springframework.web.filter.CorsFilter;
  
  import java.util.Arrays;
  
  @Configuration
  public class CorsConfig {
      @Bean
      public CorsConfiguration corsConfiguration() {
          CorsConfiguration config = new CorsConfiguration();
          config.setAllowCredentials(true);
          config.addAllowedOrigin("http://localhost:3000");
          config.addAllowedHeader("*");
          // 明确指定允许的 HTTP 方法
          config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
          config.setMaxAge(3600L);
          config.addExposedHeader("Authorization");
          config.addExposedHeader("X-XSRF-TOKEN");
          return config;
      }
  
      @Bean
      public CorsFilter corsFilter() {
          UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
          source.registerCorsConfiguration("/**", corsConfiguration());
          return new CorsFilter(source);
      }
  }
  ```

- ***config cors in security spring***
  
  ```java
  // SecurityConfig.java
  @Configuration
  @EnableWebSecurity
  
      private  CorsFilter corsFilter;
      private  CorsConfiguration corsConfiguration;
  
  http.addFilterBefore(corsFilter, SessionManagementFilter.class)
      .cors(cors -> cors.configurationSource(request -> corsConfiguration))
  ```

### Backend

#### Connect with database

+ add postgres dependency

+ config database attributes in .properties
  
  ```properties
  spring.application.name=cloud-storage
  spring.datasource.url=jdbc:postgresql://localhost:5432/cloud-storage
  spring.datasource.username=postgres
  spring.datasource.password=123456
  
  spring.jpa.hibernate.ddl-auto=update
  spring.jpa.hibernate.boot.allow_jdbc_metadata_access=false
  spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
  spring.jpa.properties.hibernate.format_sql=true
  
  spring.show_sql=true
  
  spring.sql.init.mode=always
  ```

+ write schema.sql and connect with true database

+ create identical Entity Class

+ create identical Mapper Interface
  
  + because we use MyBatis, and the mapping will be like this
    
    ```java
    @Select("SELECT * FROM USERS WHERE username = #{username}")
        @Results({
                @Result(property = "userid", column = "userid"),
                @Result(property = "username", column = "username"),
                @Result(property = "salt", column = "salt"),
                @Result(property = "password", column = "password"),
                @Result(property = "firstname", column = "firstname"),
                @Result(property = "lastname", column = "lastname")
        })
        User findByUsername(String username);
    ```

+ we need to pay attention that the properities name in the entity doesn't need to be identical with the column name in the database, cause  in mapper, the relationship will be declared .

#### How to use spring security to verify the username and password

- ***the security config for login verification***

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private AppUserDetailsService userDetailsService;
    private AppAuthenticationFailureHandler failureHandler;
    private AppLogoutHandler logoutHandler;
    private  CorsFilter corsFilter;
    private  CorsConfiguration corsConfiguration;

    @Autowired
    public SecurityConfig(AppUserDetailsService userDetailsService, AppAuthenticationFailureHandler failureHandler, AppLogoutHandler logoutHandler, CorsFilter corsFilter, CorsConfiguration corsConfiguration) {
        this.userDetailsService = userDetailsService;
        this.failureHandler = failureHandler;
        this.logoutHandler = logoutHandler;
        this.corsFilter = corsFilter;
        this.corsConfiguration = corsConfiguration;
    }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
      http.addFilterBefore(corsFilter, SessionManagementFilter.class)
              .csrf(csrf -> csrf.disable())
              .cors(cors -> cors.configurationSource(request -> corsConfiguration))
              .authorizeHttpRequests(authorize -> authorize
                      .requestMatchers("/signup",  
                        "/login",// make sure include login page URL
                        "/css/**",
                        "/js/**",
                        "/h2-console/**")
                      .permitAll()
                      .anyRequest()
                      .authenticated()
              )
              .formLogin(form -> form
                      .loginPage("/login")
                      .failureHandler(failureHandler)
                      .defaultSuccessUrl("/home", true)
              )
              .logout(logout -> logout
                      .logoutUrl("/logout")
                      .logoutSuccessHandler(logoutHandler)
                      .logoutSuccessUrl("/login?logoutMessage=You have been logged out")// 定义登出 URL
                      .invalidateHttpSession(true) // invalidate session
                      .clearAuthentication(true) // clean authentication information
              );

      return http.build();
  }

  @Bean
  //AuthenticationConfiguration will collect Provider automatically,included the one we registered
  public AuthenticationManager authenticationManager
  (AuthenticationConfiguration authenticationConfiguration) throws Exception {
 return authenticationConfiguration.getAuthenticationManager();
 }

  @Bean
  public DaoAuthenticationProvider providerManager()throws Exception {
 DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
 provider.setUserDetailsService(userDetailsService);
 provider.setPasswordEncoder(getPasswordEncoder());
 return provider;
 }

  @Bean
  public PasswordEncoder getPasswordEncoder() {
      return new BCryptPasswordEncoder(16);
  }
  } 
```

+ ***The verification logic***   ![image](D:\JobHunting-project\ucadity\Project\SuperDuperDrive-7.jpg)

#### Signup Page

> It looks like the login page. But there are some differences.

+ ***The frontend page`handleSubmit`***

```jsx
const handleSubmit = async (e)=>{
        e.preventDefault();
        if (!username || !password || !firstname || !lastname) {
            alert('you have to fill all the fields！');
            return;
        }

        const userData = {
            username: username.trim(),
            password: password.trim(),
            firstname: firstname.trim(),
            lastname: lastname.trim()
        };

        console.log('the sending data:', userData);

        try {
            const response = await fetch(
                'http://localhost:8080/signup', {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json',
                    },
                    body: JSON.stringify(userData)
                });

            //get the authentication response
            if (response.ok){
                const data = await response.json();
                localStorage.setItem("token","");
                navigate("/login")
            }

        }catch (error){
            console.error('Signup failed:', error);
        }

    }
```

+ `JSON.stringify()` serializes the `userData` object into a JSON string and sends it as the request body to the server.

+ In Spring Boot, the typical way to receive JSON data is by using the `@RequestBody` annotation. 

+ Spring Boot will automatically deserialize the JSON data into a Java object (such as `User`), provided that the request header includes `Content-Type: application/json` and the `User` class has fields matching the JSON data along with appropriate getter and setter methods. 

+ JSON is better suited for handling complex data structures, such as nested objects or arrays.

+ ***The backend page***

```java
@Controller
public class SignupController {
    @Autowired
    private UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@RequestBody User user) {
        System.out.println(user.getFirstname());
        boolean registered = userService.register(user);
        Map<String, String> response = new HashMap<>();
        if (registered) {
            response.put("message", "successfully registered!");
            return  ResponseEntity.ok().body(response);
        }else{
            response.put("message", "failed");
            return  ResponseEntity.badRequest().body(response);
        }

    }
}

@Service
public class UserService {
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder encoder;


    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public boolean register(User user){
        User findUser = userMapper.findByUsername(user.getUsername());
        if(findUser != null){
            System.out.println("User already exists");
        }else{
            System.out.println("username:"+user.getUsername());
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                throw new IllegalArgumentException("the password can not be empty");
            }
            user.setPassword(encoder.encode(user.getPassword()));
            int i = userMapper.insertUser(user);
            System.out.println(i);
            if(i == 1){
                return true;
            }
        }
        return false;
    }

}

//the send back ResponseEntity
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 42

{
    "message": "successfully registered!"
}
```
