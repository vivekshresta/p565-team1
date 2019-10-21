import React from "react";
import { Link } from "react-router-dom";
// @material-ui/core components
import { makeStyles } from "@material-ui/core/styles";
import InputAdornment from "@material-ui/core/InputAdornment";
import Icon from "@material-ui/core/Icon";
// @material-ui/icons
import Email from "@material-ui/icons/Email";
// core components
import Header from "components/Header/Header.js";
import HeaderLinks from "components/Header/HeaderLinks.js";
import GridContainer from "components/Grid/GridContainer.js";
import GridItem from "components/Grid/GridItem.js";
import Button from "components/CustomButtons/Button.js";
import Card from "components/Card/Card.js";
import CardBody from "components/Card/CardBody.js";
import CardHeader from "components/Card/CardHeader.js";
import CardFooter from "components/Card/CardFooter.js";
import CustomInput from "components/CustomInput/CustomInput.js";
import TypeSelect from "views/Modals/TypeSelect.js";
import FacebookLogin from 'react-facebook-login/dist/facebook-login-render-props'

import loginStyles from "assets/jss/material-kit-react/views/loginPage.js";

import image from "assets/img/bg7.jpg";

const useLoginStyles = makeStyles(loginStyles);

export default function LoginPage(props) {
  const [cardAnimaton, setCardAnimation] = React.useState("cardHidden");
  setTimeout(function() {
    setCardAnimation("");
  }, 700);
  const loginClasses = useLoginStyles();
  const { ...rest } = props;
  const responseFacebook = (response) => {
  console.log(response);
}
  return (
    <div>
      <Header
        absolute
        color="white"
        brand="InfinityCare"
        rightLinks={<HeaderLinks />}
        {...rest}
      />
      <div
        className={loginClasses.pageHeader}
        style={{
          backgroundImage: "url(" + image + ")",
          backgroundSize: "cover",
          backgroundPosition: "top center"
        }}
      >
        <div className={loginClasses.container}>
          <GridContainer justify="center">
            <GridItem xs={12} sm={12} md={4}>
              <Card className={loginClasses[cardAnimaton]}>
                <form className={loginClasses.form}>
                  <CardHeader color="primary" className={loginClasses.cardHeader}>
                    <h4>Log in with</h4>
                    <div className={loginClasses.socialLine}>
                      <FacebookLogin
                        appId="523513645103749"
                        autoLoad={false}
                        fields="name,email,picture"
                        callback={responseFacebook}
                        render={renderProps => ( 
                          <Button
                            justIcon
                            target="_blank"
                            color="transparent"
                            onClick={renderProps.onClick}
                          >
                            <i className={loginClasses.socialIcons + " fab fa-facebook"} />
                          </Button>
                        )}
                      />
                    </div>
                  </CardHeader>
                  <CardBody>
                    <div style={{display: 'flex', justifyContent: 'center', alignItems: 'center', marginBottom: 10}}>
                      <p style={{display: 'flex', justifyContent: 'center', margin: 0}}>Don't have an account?</p>
                      <TypeSelect />
                    </div>
                    <CustomInput
                      labelText="Email..."
                      id="email"
                      formControlProps={{
                        fullWidth: true
                      }}
                      inputProps={{
                        type: "email",
                        endAdornment: (
                          <InputAdornment position="end">
                            <Email className={loginClasses.inputIconsColor} />
                          </InputAdornment>
                        )
                      }}
                    />
                    <CustomInput
                      labelText="Password"
                      id="password"
                      formControlProps={{
                        fullWidth: true
                      }}
                      inputProps={{
                        type: "password",
                        endAdornment: (
                          <InputAdornment position="end">
                            <Icon className={loginClasses.inputIconsColor}>
                              lock_outline
                            </Icon>
                          </InputAdornment>
                        ),
                        autoComplete: "off"
                      }}
                    />
                    <div style={{display: 'flex', justifyContent: 'right', alignItems: 'center', marginBottom: 10}}>
                      <Button color="primary" simple>
                        Forgot password?
                      </Button>
                    </div>
                    <small style={{display: 'flex', justifyContent: 'center'}}>I agree to the Terms and Conditions &amp; Privacy Policy</small>
                  </CardBody>
                  <CardFooter className={loginClasses.cardFooter}>
                    <Button color="primary" size="lg">
                      Sign In
                    </Button>
                  </CardFooter>
                </form>
              </Card>
            </GridItem>
          </GridContainer>
        </div>
      </div>
    </div>
  );
}