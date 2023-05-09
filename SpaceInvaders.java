import tester.Tester;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import javalib.worldimages.*;
import javalib.funworld.*;
import java.awt.Color;
import java.util.Random;

class GameScene extends World {
  int width;
  int height;
  User user;
  IList<Invader> invaders;
  IList<Bullet> userBullets;
  IList<Bullet> invaderBullets;
  Random rand;

  GameScene(int width, int height, User user, IList<Invader> invaders, IList<Bullet> userBullets,
      IList<Bullet> invaderBullets) {
    this.width = width;
    this.height = height;
    this.user = user;
    this.invaders = invaders;
    this.userBullets = userBullets;
    this.invaderBullets = invaderBullets;
    this.rand = new Random();
  }

  /* Template:
   *  Fields:
   *    this.width ...          -- int
   *    this.height ...         -- int
   *    this.user ...           -- User
   *    this.invaders ...       -- IList<Invader>
   *    this.userBullets ...    -- IList<Bullet>
   *    this.invaderBullets ... -- IList<Bullets>
   *  Methods:
   *    this.makeUserScene() ...      -- WorldScene
   *    this.makeInvaderScene() ...   -- WorldScene
   *    this.makeUBulletsScene() ...  -- WorldScene
   *    this.makeScene() ...          -- WorldScene
   *    this.onTick() ...             -- World
   *    this.onKeyEvent(String) ...   -- World
   *    this.worldEnds() ...          -- WorldEnd
   *    this.lastScene() ...          -- WorldScene 
   *  Methods of Fields:
   *    this.invaders.filter(Predicate<Invader>) ...            -- IList<Invader>
   *    this.invaders.andmap(Predicate<Invader>) ...            -- boolean
   *    this.invaders.ormap(Predicate<Invader>) ...             -- boolean
   *    this.invaders.map(Function<Invader, U>) ...             -- U
   *    this.invaders.fold(BiFunction<Invader, U, U) ...        -- U
   *    this.invader.draw(WorldScene) ...                       -- WorldScene
   *    this.userBullets.filter(Predicate<Bullets>) ...         -- IList<Bullets>
   *    this.userBullets.andmap(Predicate<Bullets>) ...         -- boolean
   *    this.userBullets.ormap(Predicate<Bullets>) ...          -- boolean
   *    this.userBullets.map(Function<Bullets, U>) ...          -- U
   *    this.userBullets.fold(BiFunction<Bullets, U, U) ...     -- U
   *    this.userBullets.draw(WorldScene) ...                   -- WorldScene
   *    this.invaderBullets.filter(Predicate<Bullets>) ...      -- IList<Bullets>
   *    this.invaderBullets.andmap(Predicate<Bullets>) ...      -- boolean
   *    this.invaderBullets.ormap(Predicate<Bullets>) ...       -- boolean
   *    this.invaderBullets.map(Function<Bullets, U>) ...       -- U
   *    this.invaderBullets.fold(BiFunction<Bullets, U, U) ...  -- U
   *    this.invaderBullets.draw(WorldScene) ...                -- WorldScene
   */

  //returns world scene with user
  public WorldScene makeUserScene() {
    return user.drawGamePiece(new WorldScene(this.width, this.height));
  }

  //returns world scene with invaders and user
  public WorldScene makeInvaderScene() {
    return invaders.fold(new InvaderFoldHelper(), this.makeUserScene());
  }

  //returns worldscene with bullets invaders and users
  public WorldScene makeUBulletsScene() {
    return userBullets.fold(new BulletFoldHelper(), this.makeInvaderScene());
  }

  //returns the worldscene with all elements
  public WorldScene makeScene() {
    return invaderBullets.fold(new BulletFoldHelper(), this.makeUBulletsScene());
  }

  //runs every tick
  public World onTick() {
    int shooter = rand.nextInt(100);
    IList<Invader> fInvaders = this.invaders.filter(new InvaderExists(userBullets));
    IList<Bullet> currentBullets = this.userBullets.filter(new BulletContact(this.invaders));

    if (this.invaderBullets.filter(new InScreen()).listSmallerThan(10)
        && shooter < fInvaders.listLength()) {
      return new GameScene(this.width, this.height, this.user,
          this.invaders.filter(new InvaderExists(userBullets)),
          currentBullets.map(new MoveBullets()),
          new ConsList<Bullet>(
              new Bullet(fInvaders.filter(new InvaderShooting()).index(shooter).location, false),
              this.invaderBullets.map(new MoveBullets())));
    }
    else {
      return new GameScene(this.width, this.height, this.user,
          this.invaders.filter(new InvaderExists(userBullets)),
          currentBullets.map(new MoveBullets()), this.invaderBullets.map(new MoveBullets()));
    }
  }

  //checks for keyboard events
  public World onKeyEvent(String key) {
    if (key.equals("left") && this.user.location.x > 25) {
      return new GameScene(this.width, this.height, this.user.moveLeft(), this.invaders,
          this.userBullets, this.invaderBullets);
    }
    else if (key.equals("right") && this.user.location.x < 675) {
      return new GameScene(this.width, this.height, this.user.moveRight(), this.invaders,
          this.userBullets, this.invaderBullets);
    }
    else if (key.equals(" ") && this.userBullets.filter(new InScreen()).listSmallerThan(3)) {
      return new GameScene(this.width, this.height, this.user, this.invaders,
          new ConsList<Bullet>(
              new Bullet(new CartPt(this.user.location.x, this.user.location.y - 18), true),
              this.userBullets),
          this.invaderBullets);
    }
    else {
      return this;
    }
  }

  //checks if world ends
  public WorldEnd worldEnds() {
    if (this.user.hitByBullet(invaderBullets)
        || this.invaders.filter(new InvaderExists(userBullets)).listSmallerThan(0)) {
      return new WorldEnd(true, this.lastScene());
    }
    else {
      return new WorldEnd(false, this.makeScene());
    }
  }

  //returns worldScene of endstate
  public WorldScene lastScene() {
    if (this.user.hitByBullet(invaderBullets)) {
      return new WorldScene(this.width, this.height)
          .placeImageXY(new TextImage("You Lose", 24, FontStyle.BOLD, Color.RED), 350, 250);
    }
    else if (this.invaders.filter(new InvaderExists(userBullets)).listSmallerThan(0)) {
      return new WorldScene(this.width, this.height)
          .placeImageXY(new TextImage("You Win", 24, FontStyle.BOLD, Color.GREEN), 350, 250);
    }
    else {
      return new WorldScene(this.width, this.height).placeImageXY(
          new TextImage("Unable to Complete Game", 24, FontStyle.BOLD, Color.RED), 350, 250);
    }
  }

}

class CartPt {
  int x;
  int y;

  CartPt(int x, int y) {
    this.x = x;
    this.y = y;
  }

  /* Template:
   *  Fields:
   *    this.x ... -- int
   *    this.y ... -- int
   */
}

interface IGamePiece {
  WorldScene drawGamePiece(WorldScene w);
}

class User implements IGamePiece {
  CartPt location;
  boolean alive;

  User(CartPt location, boolean alive) {
    this.location = location;
    this.alive = alive;
  }

  /* Template:
   *  Fields:
   *    this.location ...   -- CartPt
   *    this.isShooting ... -- boolean
   *    this.isMoving ...   -- boolean
   *    this.direction ...  -- boolean
   *  Methods:
   *    this.drawGamePiece(WorldScene) ... -- WorldScene
   *    this.makeScene() ...               -- WorldScene
   *    this.moveLeft() ...                -- User
   *    this.moveRight() ...               -- User
   *    this.hitByBullet() ...             -- Boolean 
   */

  //draws game piece
  public WorldScene drawGamePiece(WorldScene w) {
    return w.placeImageXY(new RectangleImage(30, 30, "solid", Color.blue), this.location.x,
        this.location.y);
  }

  // draws spaceship
  public WorldScene makeScene() {
    return null;
  }

  //returns new user to left
  User moveLeft() {
    return new User(new CartPt(this.location.x - 10, this.location.y), true);
  }

  //returns a new user to the right
  User moveRight() {
    return new User(new CartPt(this.location.x + 10, this.location.y), true);
  }

  //returns true if user hit by bullet
  public boolean hitByBullet(IList<Bullet> loB) {
    return loB.map(new BulletLocation()).ormap(new ContainsPoint(this.location));
  }

}

class Invader implements IGamePiece {
  CartPt location;
  boolean activeShooter;
  boolean alive;
  int id;

  Invader(CartPt location, boolean activeShooter, boolean alive, int id) {
    this.location = location;
    this.activeShooter = activeShooter;
    this.alive = alive;
    this.id = id;
  }

  /* Template:
   *  Fields:
   *    this.location ...   -- CartPt
   *    this.isShooting ... -- boolean
   *  Methods:
   *    this.drawGamePiece(WorldScene) ... -- WorldScene
   *    this.makeScene() ...               -- WorldScene
   *    this.hitByBullet(IList<Bullet>) ...-- Invader
   * 
   */

  //draws game piece on world scene
  public WorldScene drawGamePiece(WorldScene w) {
    return w.placeImageXY(new RectangleImage(30, 30, "solid", Color.red), this.location.x,
        this.location.y);
  }

  public WorldScene makeScene() {
    // TODO Auto-generated method stub
    return null;
  }

  //returns an invader if hit by bullet that is no longer alive
  public Invader hitByBullet(IList<Bullet> loB) {
    if (!(this.alive) || loB.map(new BulletLocation()).ormap(new ContainsPoint(this.location))) {
      return new Invader(this.location, this.activeShooter, false, this.id);
    }
    else {
      return this;
    }
  }

  //returns random invader location
  public CartPt randomInvaderLocation(int x, IList<Invader> loI) {
    return loI.index(x).location;
  }

}

class Bullet implements IGamePiece {
  CartPt location;
  boolean type;

  Bullet(CartPt location, boolean type) {
    this.location = location;
    this.type = type;
  }

  /* Template:
   *  Fields:
   *    this.location ...   -- CartPt
   *    this.type ...       -- boolean
   *  Methods:
   *    this.drawGamePiece(WorldScene) ... -- WorldScene
   *    this.moveBulletHelper() ...        -- Bullet
   *    this.createUserBullet(User) ...    -- Bullet
   *    this.bulletHit(IList<Invader>) ... -- Boolean
   * 
   */

  //draws game piece
  public WorldScene drawGamePiece(WorldScene w) {
    if (this.type) {
      return w.placeImageXY(new EquilateralTriangleImage(10, "solid", Color.black), this.location.x,
          this.location.y);
    }
    else {
      return w.placeImageXY(new CircleImage(4, "solid", Color.black), this.location.x,
          this.location.y);
    }
  }

  //moves bullet for users and invaders
  public Bullet moveBulletHelper() {
    if (this.type) {
      return new Bullet(new CartPt(this.location.x, this.location.y - 2), true);
    }
    else {
      return new Bullet(new CartPt(this.location.x, this.location.y + 2), false);
    }
  }

  //creates a bullet at user location
  public Bullet createUserBullet(User u) {
    return new Bullet(new CartPt(u.location.x, u.location.y), true);
  }

  //retruns if bullet hit an invader
  public boolean bulletHit(IList<Invader> loI) {
    return loI.map(new InvaderLocation()).ormap(new ContainsPoint(this.location));
  }

}

interface IList<T> {
  // filter this IList using the given predicate
  IList<T> filter(Predicate<T> pred);

  // returns true if any member in IList passes the predicate
  Boolean ormap(Predicate<T> pred);

  // returns true if every member in IList passes the predicate
  Boolean andmap(Predicate<T> pred);

  // map the given function onto every member of this IList
  <U> IList<U> map(Function<T, U> converter);

  // combine the items in this IList using the given function
  <U> U fold(BiFunction<T, U, U> converter, U initial);

  // draw the dots in this ILoDot onto the scene
  WorldScene draw(WorldScene acc);

  //returns list length
  int listLength();

  //returns if this list is smaller than given int
  boolean listSmallerThan(int x);

  T index(int x);

}

class MtList<T> implements IList<T> {

  MtList() {
  }

  /* Template:
   *  Fields:
   *  Methods:
   *    this.filter(Predicate<T>) ...       -- IList<T>
   *    this.ormap(Predicate<T>) ...        -- boolean
   *    this.andmap(Predicate<T>) ...       -- boolean
   *    this.map(Function<T, U>) ...        -- IList<U>
   *    this.fold(BiFunction<T, U, U>) ...  -- U
   *    this.draw(WorldScene) ...           -- WorldScene
   *    this.listLength() ...               -- int
   *    boolean llistSmallerThan(int) ...   -- boolean
   */

  // filter this MtList using the given predicate
  public IList<T> filter(Predicate<T> pred) {
    return new MtList<T>();
  }

  public Boolean ormap(Predicate<T> pred) {
    return false;
  }

  public Boolean andmap(Predicate<T> pred) {
    return true;
  }

  // map the given function onto every member of this MtList
  public <U> IList<U> map(Function<T, U> converter) {
    return new MtList<U>();
  }

  // combine the items in this MtList using the given function
  public <U> U fold(BiFunction<T, U, U> converter, U initial) {
    return initial;
  }

  @Override
  public WorldScene draw(WorldScene acc) {
    return acc;
  }

  @Override
  //returns list length
  public int listLength() {
    return 0;
  }

  @Override
  //returns if the list is smaller than given int
  public boolean listSmallerThan(int x) {
    return true;
  }

  @Override
  public T index(int x) {
    return null;
  }

}

class ConsList<T> implements IList<T> {
  T first;
  IList<T> rest;

  ConsList(T first, IList<T> rest) {
    this.first = first;
    this.rest = rest;
  }

  /* Template:
   *  Fields:
   *    this.first ...  -- T
   *    this.rest ...   -- IList<T>
   *  Methods:
   *    this.filter(Predicate<T>) ...       -- IList<T>
   *    this.ormap(Predicate<T>) ...        -- boolean
   *    this.andmap(Predicate<T>) ...       -- boolean
   *    this.map(Function<T, U>) ...        -- IList<U>
   *    this.fold(BiFunction<T, U, U>) ...  -- U
   *    this.draw(WorldScene) ...           -- WorldScene
   *    this.listLength() ...               -- int
   *    this.listSmallerThan(int) ...       -- boolean
   *  Methods of Fields
   *    this.rest.filter(Predicate<T>) ...       -- IList<T>
   *    this.rest.ormap(Predicate<T>) ...        -- boolean
   *    this.rest.andmap(Predicate<T>) ...       -- boolean
   *    this.rest.map(Function<T, U>) ...        -- IList<U>
   *    this.rest.fold(BiFunction<T, U, U>) ...  -- U
   *    this.rest.draw(WorldScene) ...           -- WorldScene
   *    this.rest.listLength() ...               -- int
   *    this.rest.listSmallerThan(int) ...       -- boolean
   *    
   */

  // filter this ConsList using the given predicate
  public IList<T> filter(Predicate<T> pred) {
    if (pred.test(this.first)) {
      return new ConsList<T>(this.first, this.rest.filter(pred));
    }
    else {
      return this.rest.filter(pred);
    }
  }

  @Override
  //ors all items in mapped list
  public Boolean ormap(Predicate<T> pred) {
    if (pred.test(this.first)) {
      return true;
    }
    else {
      return this.rest.ormap(pred);
    }
  }

  //ands all items in a mapped list
  public Boolean andmap(Predicate<T> pred) {
    if (pred.test(this.first)) {
      return this.rest.andmap(pred);
    }
    else {
      return false;
    }
  }

  // map the given function onto every member of this ConsList
  public <U> IList<U> map(Function<T, U> converter) {
    return new ConsList<U>(converter.apply(this.first), this.rest.map(converter));
  }

  // combine the items in this ConsList using the given function
  public <U> U fold(BiFunction<T, U, U> converter, U initial) {
    return converter.apply(this.first, this.rest.fold(converter, initial));
  }

  @Override
  public WorldScene draw(WorldScene acc) {
    return null;
  }

  @Override
  //return list length
  public int listLength() {
    return 1 + this.rest.listLength();
  }

  @Override
  //return if list is smaller than given length
  public boolean listSmallerThan(int x) {
    return (this.listLength() < x);
  }

  @Override
  public T index(int x) {
    if (x == 0) {
      return this.first;
    }
    else {
      return this.rest.index(x - 1);
    }
  }

}

// Helper Predicates
class InvaderFoldHelper implements BiFunction<Invader, WorldScene, WorldScene> {

  public WorldScene apply(Invader t, WorldScene u) {
    return t.drawGamePiece(u);
  }

}

//helps bullet Folder
class BulletFoldHelper implements BiFunction<Bullet, WorldScene, WorldScene> {

  public WorldScene apply(Bullet t, WorldScene u) {
    return t.drawGamePiece(u);
  }

}

//filters list for contained point
class ContainsPoint implements Predicate<CartPt> {
  CartPt givenPoint;

  ContainsPoint(CartPt givenPoint) {
    this.givenPoint = givenPoint;
  }

  @Override
  public boolean test(CartPt t) {
    return t.x > (givenPoint.x - 18) && t.x < (givenPoint.x + 18) && t.y > (givenPoint.y - 18)
        && t.y < (givenPoint.y + 18);
  }
}

//moves bullets
class MoveBullets implements Function<Bullet, Bullet> {

  public Bullet apply(Bullet b) {
    return b.moveBulletHelper();
  }
}

//filter for bullets in screen
class InScreen implements Predicate<Bullet> {
  public boolean test(Bullet b) {
    return (b.location.x > 0 && b.location.x < 700 && b.location.y > 0 && b.location.y < 500);
  }
}

//filters existing invaders
class InvaderExists implements Predicate<Invader> {
  IList<Bullet> givenBullets;

  InvaderExists(IList<Bullet> givenBullets) {
    this.givenBullets = givenBullets;
  }

  public boolean test(Invader i) {
    return i.hitByBullet(givenBullets).alive;
  }
}

//filters bullets that contacted invaders
class BulletContact implements Predicate<Bullet> {
  IList<Invader> givenInvaders;

  BulletContact(IList<Invader> givenInvaders) {
    this.givenInvaders = givenInvaders;
  }

  public boolean test(Bullet b) {
    return !(b.bulletHit(givenInvaders));
  }
}

// converts bullets to locations
class BulletLocation implements Function<Bullet, CartPt> {

  public CartPt apply(Bullet b) {
    return b.location;
  }
}

// converts invaders to locations
class InvaderLocation implements Function<Invader, CartPt> {

  public CartPt apply(Invader i) {
    return i.location;
  }
}

//converts invaders to bullets
class InvaderToBullet implements Function<Invader, Bullet> {

  public Bullet apply(Invader t) {
    return new Bullet(t.location, false);
  }
}

//filters invaders shooting
class InvaderShooting implements Predicate<Invader> {
  public boolean test(Invader i) {
    return !(i.activeShooter);
  }
}

// Utils Class
class Utils {

  //creates a list of invaders
  IList<Invader> buildInvaders(int total, int count, int xPos, int yPos, int width, int height) {
    if (count < total && xPos < width) {
      return new ConsList<Invader>(new Invader(new CartPt(xPos, yPos), false, true, count + 1),
          buildInvaders(total, count + 1, xPos + 50, yPos, width, height));
    }
    else if (count < total && xPos >= width) {
      return new ConsList<Invader>(
          new Invader(new CartPt(xPos - width + 50, yPos + 50), false, true, count + 1),
          buildInvaders(total, count + 1, xPos - width + 50, yPos + 50, width, height));
    }
    else {
      return new MtList<Invader>();
    }
  }
}

// Example Class
class ExampleSpaceInvader {
  //Test objects
  CartPt point2020 = new CartPt(20, 20);

  IList<CartPt> mt = new MtList<CartPt>();
  IList<CartPt> cp1 = new ConsList<CartPt>(new CartPt(10, 10), mt);
  IList<CartPt> cp2 = new ConsList<CartPt>(new CartPt(20, 20), cp1);
  IList<CartPt> points = new ConsList<CartPt>(new CartPt(30, 30), cp2);

  IList<Boolean> b1 = new ConsList<Boolean>(false,
      new ConsList<Boolean>(true, new ConsList<Boolean>(false, new MtList<Boolean>())));

  User user1 = new User(new CartPt(300, 400), true);
  Invader enemyShip1 = new Invader(new CartPt(58, 50), false, true, 1);
  /*
  Invader enemyShip2 = new Invader(new CartPt(131, 50), false, true, 2);
  Invader enemyShip3 = new Invader(new CartPt(204, 50), false, true, 3);
  Invader enemyShip4 = new Invader(new CartPt(277, 50), false, true, 4);
  Invader enemyShip5 = new Invader(new CartPt(350, 50), false, true, 5);
  Invader enemyShip6 = new Invader(new CartPt(423, 50), false, true, 6);
  Invader enemyShip7 = new Invader(new CartPt(496, 50), false, true, 7);
  Invader enemyShip8 = new Invader(new CartPt(569, 50), false, true, 8);
  Invader enemyShip9 = new Invader(new CartPt(642, 50), false, true, 9);
  Invader enemyShip10 = new Invader(new CartPt(58, 115), false, true, 10);
  Invader enemyShip11 = new Invader(new CartPt(131, 115), false, true, 11);
  Invader enemyShip12 = new Invader(new CartPt(204, 115), false, true, 12);
  Invader enemyShip13 = new Invader(new CartPt(277, 115), false, true, 13);
  Invader enemyShip14 = new Invader(new CartPt(350, 115), false, true, 14);
  Invader enemyShip15 = new Invader(new CartPt(423, 115), false, true, 15);
  Invader enemyShip16 = new Invader(new CartPt(496, 115), false, true, 16);
  Invader enemyShip17 = new Invader(new CartPt(569, 115), false, true, 17);
  Invader enemyShip18 = new Invader(new CartPt(642, 115), false, true, 18);
  IList<Invader> mtInvader = new MtList<Invader>();
  IList<Invader> LoInvaders = new ConsList<Invader>(enemyShip1, new ConsList<Invader>(enemyShip2,
      new ConsList<Invader>(enemyShip3, new ConsList<Invader>(enemyShip4, new ConsList<Invader>(
          enemyShip5,
          new ConsList<Invader>(enemyShip6, new ConsList<Invader>(enemyShip7,
              new ConsList<Invader>(enemyShip8, new ConsList<Invader>(enemyShip9,
                  new ConsList<Invader>(enemyShip10, new ConsList<Invader>(enemyShip11,
                      new ConsList<Invader>(enemyShip12, new ConsList<Invader>(enemyShip13,
                          new ConsList<Invader>(enemyShip14, new ConsList<Invader>(enemyShip15,
                              new ConsList<Invader>(enemyShip16, new ConsList<Invader>(enemyShip17,
                                  new ConsList<Invader>(enemyShip18, mtInvader))))))))))))))))));
  */

  //Use util class to draw invaders
  IList<Invader> testInvaderBuildList = new Utils().buildInvaders(27, 0, 50, 50, 700, 500);

  //create test user bullets
  Bullet userBullet1 = new Bullet(new CartPt(100, 200), true);
  Bullet userBullet2 = new Bullet(new CartPt(320, 270), true);
  Bullet userBullet3 = new Bullet(new CartPt(100, 450), true);
  IList<Bullet> mtUserBullet = new MtList<Bullet>();
  IList<Bullet> LoUserBullet = new ConsList<Bullet>(userBullet1,
      new ConsList<Bullet>(userBullet2, new ConsList<Bullet>(userBullet3, mtUserBullet)));

  IList<Bullet> testLoB = new ConsList<Bullet>(new Bullet(new CartPt(300, 400), false),
      new MtList<Bullet>());
  IList<Bullet> testLoB2 = new ConsList<Bullet>(new Bullet(new CartPt(58, 50), true),
      new MtList<Bullet>());

  Bullet enemyBullet1 = new Bullet(new CartPt(150, 300), false);
  Bullet enemyBullet2 = new Bullet(new CartPt(400, 300), false);
  Bullet enemyBullet3 = new Bullet(new CartPt(350, 200), false);
  IList<Bullet> mtInvaderBullet = new MtList<Bullet>();
  IList<Bullet> LoInvaderBullet = new ConsList<Bullet>(enemyBullet1,
      new ConsList<Bullet>(enemyBullet2, new ConsList<Bullet>(enemyBullet3, mtInvaderBullet)));

  GameScene testGameScene = new GameScene(700, 500, this.user1, this.testInvaderBuildList,
      this.LoUserBullet, this.LoInvaderBullet);
  WorldScene testWorld = new WorldScene(700, 500);

  //run the game
  boolean testBigBang(Tester t) {
    int worldWidth = 700;
    int worldHeight = 500;
    double tickRate = .02;
    GameScene world = new GameScene(worldWidth, worldHeight, this.user1, this.testInvaderBuildList,
        this.mtUserBullet, this.mtInvaderBullet);
    return world.bigBang(worldWidth, worldHeight, tickRate);
  }

  //test contains point
  boolean testContainsPoint(Tester t) {
    return t.checkExpect(points.ormap(new ContainsPoint(this.point2020)), true);
  }

  // GameScene Test
  //test drawing game scene
  boolean testMakeUserScene(Tester t) {
    return t.checkExpect(this.testGameScene.makeUserScene(),
        new WorldScene(700, 500).placeImageXY(new RectangleImage(30, 30, "solid", Color.blue), 300,
            400))
        && t.checkExpect(this.testGameScene.makeInvaderScene(),
            new WorldScene(700, 500)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.blue), 300, 400)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 50, 50)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 100, 50)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 150, 50)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 200, 50)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 250, 50)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 300, 50)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 350, 50)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 400, 50)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 450, 50)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 500, 50)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 550, 50)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 600, 50)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 650, 50)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 50, 100)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 100, 100)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 150, 100)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 200, 100)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 250, 100)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 300, 100)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 350, 100)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 400, 100)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 450, 100)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 500, 100)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 550, 100)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 600, 100)
                .placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 650, 100));
  }

  //ListTest
  //test get the length of list
  boolean testListLength(Tester t) {
    return t.checkExpect(this.cp1.listLength(), 1)
        && t.checkExpect(this.testInvaderBuildList.listLength(), 27)
        && t.checkExpect(this.mt.listLength(), 0);
  }

  //test if the list is smaller than given int
  boolean testListSmallerThan(Tester t) {
    return t.checkExpect(this.cp1.listSmallerThan(3), true)
        && t.checkExpect(this.testInvaderBuildList.listSmallerThan(14), false);
  }

  // User Test
  //test draw user
  boolean testUserDrawGamePiece(Tester t) {
    return t.checkExpect(this.user1.drawGamePiece(testWorld),
        this.testWorld.placeImageXY(new RectangleImage(30, 30, "solid", Color.blue), 300, 400));
  }

  //test user move
  boolean testUserMove(Tester t) {
    return t.checkExpect(this.user1.moveLeft(), new User(new CartPt(290, 400), true))
        && t.checkExpect(this.user1.moveRight(), new User(new CartPt(310, 400), true));
  }

  //test user hit by bullet
  boolean testUserHitByBullet(Tester t) {
    return t.checkExpect(this.user1.hitByBullet(this.LoInvaderBullet), false)
        && t.checkExpect(this.user1.hitByBullet(this.testLoB), true);
  }

  // Invader Test
  boolean testInvaderDrawGamePiece(Tester t) {
    return t.checkExpect(this.enemyShip1.drawGamePiece(testWorld),
        this.testWorld.placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 58, 50));
  }

  //test invader hit by bullet
  boolean testInvaderHitByBullet(Tester t) {
    return t.checkExpect(this.enemyShip1.hitByBullet(this.LoUserBullet),
        new Invader(new CartPt(58, 50), false, true, 1))
        && t.checkExpect(this.enemyShip1.hitByBullet(this.testLoB2),
            new Invader(new CartPt(58, 50), false, false, 1));
  }

  // Bullet Test
  // test bullet drawing
  boolean testBulletDrawGamePiece(Tester t) {
    return t.checkExpect(this.enemyBullet1.drawGamePiece(testWorld),
        this.testWorld.placeImageXY(new CircleImage(4, "solid", Color.black), 150, 300));
  }

  //test bullet move
  boolean testBulletMoveHelper(Tester t) {
    return t.checkExpect(this.userBullet1.moveBulletHelper(),
        new Bullet(new CartPt(100, 198), true));
  }

  //Predicate Test
  boolean testInvaderFoldHelper(Tester t) {
    InvaderFoldHelper ifh = new InvaderFoldHelper();
    BulletFoldHelper bfh = new BulletFoldHelper();
    ContainsPoint cp = new ContainsPoint(this.point2020);
    MoveBullets mb = new MoveBullets();
    InScreen is = new InScreen();
    InvaderExists ie = new InvaderExists(this.LoUserBullet);
    BulletContact bc = new BulletContact(this.testInvaderBuildList);
    BulletLocation bl = new BulletLocation();
    InvaderLocation il = new InvaderLocation();
    return t.checkExpect(ifh.apply(this.enemyShip1, this.testWorld),
        new WorldScene(700, 500).placeImageXY(new RectangleImage(30, 30, "solid", Color.red), 58,
            50))
        && t.checkExpect(bfh.apply(enemyBullet1, testWorld),
            new WorldScene(700, 500).placeImageXY(new CircleImage(4, "solid", Color.black), 150,
                300))
        && t.checkExpect(cp.test(new CartPt(18, 18)), true)
        && t.checkExpect(cp.test(new CartPt(0, 0)), false)
        && t.checkExpect(mb.apply(this.enemyBullet1), new Bullet(new CartPt(150, 302), false))
        && t.checkExpect(is.test(this.enemyBullet1), true)
        && t.checkExpect(ie.test(this.enemyShip1), true)
        && t.checkExpect(bc.test(this.userBullet1), true)
        && t.checkExpect(bl.apply(this.enemyBullet1), new CartPt(150, 300))
        && t.checkExpect(il.apply(enemyShip1), new CartPt(58, 50));
  }

}
